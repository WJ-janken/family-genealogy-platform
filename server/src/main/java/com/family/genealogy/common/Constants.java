package com.family.genealogy.common;

/**
 * 系统常量
 */
public class Constants {
    
    /** 角色 */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_GUEST = "GUEST";

    /** 审核状态 */
    public static final String AUDIT_DRAFT = "DRAFT";
    public static final String AUDIT_PENDING = "PENDING";
    public static final String AUDIT_APPROVED = "APPROVED";
    public static final String AUDIT_REJECTED = "REJECTED";

    /** 审核操作 */
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    /** Redis Key 前缀 */
    public static final String REDIS_TOKEN_PREFIX = "token:";
    public static final String REDIS_TREE_CACHE = "tree:cache:";
    public static final String REDIS_USER_INFO = "user:info:";

    private Constants() {}
}
