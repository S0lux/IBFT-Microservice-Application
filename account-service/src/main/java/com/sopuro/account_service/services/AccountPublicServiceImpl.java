package com.sopuro.account_service.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopuro.account_service.dtos.PrivateAccountDetailsDTO;
import com.sopuro.account_service.dtos.PublicAccountDetailsDTO;
import com.sopuro.account_service.dtos.UserDetailsDTO;
import com.sopuro.account_service.entities.AccountEntity;
import com.sopuro.account_service.entities.AccountId;
import com.sopuro.account_service.enums.AccountStatus;
import com.sopuro.account_service.enums.AccountType;
import com.sopuro.account_service.enums.UserStatus;
import com.sopuro.account_service.exceptions.account.AccountNotFound;
import com.sopuro.account_service.exceptions.account.UserStatusNotValid;
import com.sopuro.account_service.feign.AuthServiceClient;
import com.sopuro.account_service.mappers.AccountMapper;
import com.sopuro.account_service.repositories.AccountRepository;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
@Slf4j
public class AccountPublicServiceImpl implements AccountPublicService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AuthServiceClient authServiceClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final long CACHE_TTL_MINUTES = 15;
    private static final String SINGLE_ACCOUNT_CACHE_KEY_PREFIX = "account:";
    private static final String PAGED_ACCOUNTS_CACHE_KEY_PREFIX = "accounts:";
    private static final String CACHE_KEY_SEPARATOR = ":";

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    private String generateAccountNumber(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive.");
        }
        StringBuilder accountNumber = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            accountNumber.append(CHARACTERS.charAt(randomIndex));
        }
        return accountNumber.toString();
    }

    @Override
    public PrivateAccountDetailsDTO createAccount(
            UUID userId,
            UserStatus userStatus,
            AccountType type,
            BigDecimal initialBalance) throws IllegalArgumentException, UserStatusNotValid {

        if (userStatus != UserStatus.ACTIVE) {
            throw new UserStatusNotValid(userStatus);
        }

        String accountNumber = generateAccountNumber(16);
        AccountId accountId = AccountId.builder()
                .userId(userId)
                .number(accountNumber)
                .build();
        AccountEntity accountEntity = AccountEntity.builder()
                .id(accountId)
                .status(AccountStatus.NORMAL)
                .availableBalance(initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .type(type)
                .build();
        accountEntity = accountRepository.save(accountEntity);

        invalidateUserAccountsCache(userId);

        return accountMapper.toAccountDetailsDTO(accountEntity);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public PagedModel<PrivateAccountDetailsDTO> getUserPrivateAccounts(
            UUID userId,
            int page, int size) throws IllegalArgumentException, UserStatusNotValid {

        validateGetAccountsInput(userId, page, size);

        String cacheKey = buildPagedAccountsCacheKey(userId, page, size);
        PagedModel<PrivateAccountDetailsDTO> cachedResult = getPagedAccountsFromCache(cacheKey);

        if (cachedResult != null) {
            return cachedResult;
        }

        PagedModel<PrivateAccountDetailsDTO> result = queryAccountsFromDatabase(userId, page, size);
        cachePagedAccountsResultAsync(cacheKey, result);
        return result;
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public PublicAccountDetailsDTO getPublicAccountInfo(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be null or empty.");
        }

        String cacheKey = buildSingleAccountCacheKey(accountNumber);
        Optional<PublicAccountDetailsDTO> cachedResult = getSingleAccountFromCache(cacheKey);

        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        Optional<PrivateAccountDetailsDTO> result = querySingleAccountFromDatabase(accountNumber);
        result.orElseThrow(() -> new AccountNotFound(accountNumber));

        try {
            ResponseEntity<UserDetailsDTO> userDetailsDTO =
                    authServiceClient.getUserDetails(result.get().getOwnerId().toString());

            PublicAccountDetailsDTO publicAccountDetailsDTO = PublicAccountDetailsDTO.builder()
                    .number(result.get().getNumber())
                    .type(result.get().getType())
                    .status(result.get().getStatus())
                    .owner(userDetailsDTO.getBody())
                    .build();

            cacheSingleAccountResultAsync(cacheKey, publicAccountDetailsDTO);
            return publicAccountDetailsDTO;
        } catch (FeignException.NotFound e) {
            throw new AccountNotFound(accountNumber);
        }
    }

    private void validateGetAccountsInput(UUID userId, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be non-negative and size must be positive.");
        }
    }

    private String buildPagedAccountsCacheKey(UUID userId, int page, int size) {
        return PAGED_ACCOUNTS_CACHE_KEY_PREFIX + userId + CACHE_KEY_SEPARATOR + page + CACHE_KEY_SEPARATOR + size;
    }

    private String buildSingleAccountCacheKey(String accountNumber) {
        return SINGLE_ACCOUNT_CACHE_KEY_PREFIX + accountNumber;
    }

    private PagedModel<PrivateAccountDetailsDTO> getPagedAccountsFromCache(String cacheKey) {
        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null && !cachedData.isEmpty()) {
                Page<PrivateAccountDetailsDTO> cachedPage = objectMapper.readValue(
                        cachedData, new TypeReference<>() {});
                log.info("Cache hit for key: {}", cacheKey);
                return new PagedModel<>(cachedPage);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve paged accounts from cache for key {}: {}", cacheKey, e.getMessage());
        }
        return null;
    }

    private Optional<PublicAccountDetailsDTO> getSingleAccountFromCache(String cacheKey) {
        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null && !cachedData.isEmpty()) {
                PublicAccountDetailsDTO cachedAccount = objectMapper.readValue(
                        cachedData, PublicAccountDetailsDTO.class);
                log.info("Cache hit for single account key: {}", cacheKey);
                return Optional.of(cachedAccount);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve single account from cache for key {}: {}", cacheKey, e.getMessage());
        }
        return Optional.empty();
    }

    private PagedModel<PrivateAccountDetailsDTO> queryAccountsFromDatabase(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id.number").descending());
        Page<PrivateAccountDetailsDTO> result = accountRepository
                .findById_UserId(userId, pageable)
                .map(accountMapper::toAccountDetailsDTO);

        log.info("Queried {} accounts from database for user {}", result.getNumberOfElements(), userId);
        return new PagedModel<>(result);
    }

    private Optional<PrivateAccountDetailsDTO> querySingleAccountFromDatabase(String accountNumber) {
        Optional<AccountEntity> accountEntity = accountRepository.findById_Number(accountNumber);
        log.info("Queried single account from database for account number {}", accountNumber);
        return accountEntity.map(accountMapper::toAccountDetailsDTO);
    }


    @Async("taskExecutor")
    protected void cachePagedAccountsResultAsync(String cacheKey, PagedModel<PrivateAccountDetailsDTO> result) {
        try {
            String jsonToCache = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, jsonToCache, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Cached paged accounts result for key: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Failed to cache paged accounts data for key {}: {}", cacheKey, e.getMessage());
        }
    }

    @Async("taskExecutor")
    protected void cacheSingleAccountResultAsync(String cacheKey, PublicAccountDetailsDTO result) {
        try {
            String jsonToCache = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, jsonToCache, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Cached single account result for key: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Failed to cache single account data for key {}: {}", cacheKey, e.getMessage());
        }
    }

    private void invalidateUserAccountsCache(UUID userId) {
        try {
            String pluralPattern = PAGED_ACCOUNTS_CACHE_KEY_PREFIX + userId + CACHE_KEY_SEPARATOR + "*";
            Set<String> pluralKeys = redisTemplate.keys(pluralPattern);
            if (!pluralKeys.isEmpty()) {
                redisTemplate.delete(pluralKeys);
                log.info("Invalidated {} paged accounts cache entries for user {}", pluralKeys.size(), userId);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for user {}: {}", userId, e.getMessage());
        }
    }

    private void invalidateSingleAccountCache(String accountNumber) {
        try {
            String cacheKey = buildSingleAccountCacheKey(accountNumber);
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (deleted) {
                log.info("Invalidated single account cache entry for account number {}", accountNumber);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate single account cache for account number {}: {}", accountNumber, e.getMessage());
        }
    }
}