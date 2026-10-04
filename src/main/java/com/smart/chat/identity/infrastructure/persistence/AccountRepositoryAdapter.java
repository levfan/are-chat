package com.smart.chat.identity.infrastructure.persistence;

import com.smart.chat.identity.domain.account.Account;
import com.smart.chat.identity.domain.account.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link AccountRepository} 的 MyBatis-Plus 适配器，PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 更新时<b>只回写聚合持有的那几列</b>（纪律沿用 {@code CoupleSpaceRepositoryAdapter}）：
 * app_user 还有 {@code signature}/{@code presence_status} 两列不在聚合里——它们的权威副本在
 * user_profile（归 messaging），若用聚合重建整行就会把历史快照清空，所以先 {@code selectById} 拿原行、
 * 改完再写回。插入时这两列按改造前 {@code AppUser.of} 的写死默认值补齐（空串 / "online"）。
 */
@Component
public class AccountRepositoryAdapter implements AccountRepository {

    /** 插入时的历史默认值：app_user 上不在聚合内的两列 */
    private static final String LEGACY_EMPTY_SIGNATURE = "";
    private static final String LEGACY_PRESENCE_ONLINE = "online";

    private final AppUserMapper userMapper;

    public AccountRepositoryAdapter(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return userMapper.findByUsername(username).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Account> findByPhone(String phone) {
        return userMapper.findByPhone(phone).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Account> findByAccount(String account) {
        return userMapper.findByAccount(account).map(AccountRepositoryAdapter::toDomain);
    }

    @Override
    public List<Account> listAdmins() {
        return userMapper.findAdmins().stream().map(AccountRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countAdmins() {
        return userMapper.countAdmins();
    }

    @Override
    public List<Account> listSearchCandidates(String keyword, int limit) {
        return userMapper.search(keyword, limit).stream().map(AccountRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Account> listForAdmin(String keyword, int limit) {
        return userMapper.searchAll(keyword, limit).stream().map(AccountRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Account> listActiveAmongPhones(List<String> phones) {
        return userMapper.findActiveAmongPhones(phones).stream().map(AccountRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(Account account) {
        AppUserPO existing = userMapper.selectById(account.id());
        if (existing == null) {
            AppUserPO po = new AppUserPO();
            po.setId(account.id());
            applyOwnedFields(po, account);
            po.setSignature(LEGACY_EMPTY_SIGNATURE);
            po.setPresenceStatus(LEGACY_PRESENCE_ONLINE);
            userMapper.insert(po);
            return;
        }
        applyOwnedFields(existing, account);
        userMapper.updateById(existing);
    }

    /** 聚合负责维护的列；signature / presence_status 由各自的写入方负责，这里一律不碰 */
    private static void applyOwnedFields(AppUserPO po, Account account) {
        po.setUsername(account.username());
        po.setPhone(account.phone());
        po.setNickname(account.nickname());
        po.setPasswordHash(account.passwordHash());
        po.setAvatar(account.avatar());
        po.setStatus(account.status());
        po.setRole(account.role());
        po.setCreated(account.created());
        po.setLastLoginAt(account.lastLoginAt());
    }

    private static Account toDomain(AppUserPO po) {
        return Account.restore(po.getId(), po.getPhone(), po.getUsername(), po.getNickname(), po.getPasswordHash(),
                po.getAvatar(), po.getStatus(), po.getRole(), po.getCreated(), po.getLastLoginAt());
    }
}
