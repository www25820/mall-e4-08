package com.macro.mall;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.dao.UmsAdminRoleRelationDao;
import com.macro.mall.mapper.UmsAdminRoleRelationMapper;
import com.macro.mall.model.UmsAdmin;
import com.macro.mall.service.UmsAdminCacheService;
import com.macro.mall.service.UmsAdminService;
import com.macro.mall.service.impl.UmsAdminCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * E4-08 循环依赖字段注入 → 构造注入 的可运行特征测试。
 * <p>
 * 特征输入固定：调用 {@code delAdmin(adminId)}，adminId=1L，adminService.getItem 返回 username="test" 的 admin。
 * <p>
 * 关键点：本测试用 {@code new UmsAdminCacheServiceImpl(mockAdminService)} 直接构造，
 * 通过构造函数传入 adminService 替身，不启动 Spring 容器 —— 这正是构造注入替代字段注入带来的易测试性。
 * 其余字段（redisService）仍为字段注入，用 {@link ReflectionTestUtils} 塞替身（本次改造范围外，保持原样）。
 */
public class UmsAdminCacheServiceImplTest {

    private UmsAdminService adminService;
    private RedisService redisService;
    private UmsAdminCacheServiceImpl service;

    @BeforeEach
    public void setUp() {
        adminService = mock(UmsAdminService.class);
        redisService = mock(RedisService.class);
        // 构造注入：直接 new 并传入 adminService 替身（改后的构造方式）
        service = new UmsAdminCacheServiceImpl(adminService);
        // redisService 仍是字段注入，用 ReflectionTestUtils 注入替身（本次不改它）
        ReflectionTestUtils.setField(service, "redisService", redisService);
        ReflectionTestUtils.setField(service, "REDIS_DATABASE", "mall");
        ReflectionTestUtils.setField(service, "REDIS_KEY_ADMIN", "admin");
    }

    /**
     * 特征：delAdmin 在 admin 存在时，调用 adminService.getItem 并删除缓存。
     */
    @Test
    public void delAdmin_whenAdminExists_deletesCache() {
        UmsAdmin admin = new UmsAdmin();
        admin.setId(1L);
        admin.setUsername("test");
        when(adminService.getItem(1L)).thenReturn(admin);

        service.delAdmin(1L);

        // 核对：确实调用了 adminService.getItem(1L)
        verify(adminService).getItem(1L);
        // 核对：删除了对应 key（mall:admin:test）
        verify(redisService).del("mall:admin:test");
    }

    /**
     * 特征：delAdmin 在 admin 不存在（getItem 返回 null）时，不删除缓存。
     */
    @Test
    public void delAdmin_whenAdminAbsent_doesNotDelete() {
        when(adminService.getItem(1L)).thenReturn(null);

        service.delAdmin(1L);

        verify(adminService).getItem(1L);
        // 核对：admin 为 null 时不调用 redisService.del
        verify(redisService, never()).del(anyString());
    }

    /**
     * 特征：构造注入的 adminService 就是 delAdmin 实际使用的那个依赖（非 null、非 Spring 代理）。
     */
    @Test
    public void constructorInjection_wiresAdminService() {
        // 直接断言：构造传入的替身，就是 delAdmin 内部使用的 adminService
        Object field = ReflectionTestUtils.getField(service, "adminService");
        assertEquals(adminService, field, "构造注入的 adminService 应与传入替身一致");
    }

    /**
     * Spring 容器装配证据：用最小 AnnotationConfigApplicationContext，注册构造注入目标类
     * 及其依赖替身，验证容器能通过构造函数正常装配（不依赖 MySQL/Redis 外部服务）。
     */
    @Test
    public void springContainer_canWireConstructorInjection() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            // 提供 @Value 占位符（最小上下文不含 application.yml）
            Properties props = new Properties();
            props.setProperty("redis.database", "mall");
            props.setProperty("redis.expire.common", "86400");
            props.setProperty("redis.key.admin", "ums:admin");
            props.setProperty("redis.key.resourceList", "ums:resourceList");
            ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("testProps", props));

            ctx.registerBean(UmsAdminService.class, () -> adminService);
            ctx.registerBean(RedisService.class, () -> redisService);
            ctx.registerBean(UmsAdminRoleRelationMapper.class, () -> mock(UmsAdminRoleRelationMapper.class));
            ctx.registerBean(UmsAdminRoleRelationDao.class, () -> mock(UmsAdminRoleRelationDao.class));
            ctx.register(TestConfig.class);
            ctx.register(UmsAdminCacheServiceImpl.class);
            ctx.refresh();

            UmsAdminCacheService bean = ctx.getBean(UmsAdminCacheServiceImpl.class);
            assertNotNull(bean, "Spring 容器应能通过构造注入装配 UmsAdminCacheServiceImpl");
            // 验证装配进去的 adminService 就是替身
            Object wired = ReflectionTestUtils.getField(bean, "adminService");
            assertEquals(adminService, wired, "Spring 装配的 adminService 应为传入替身");
        }
    }

    @Configuration
    static class TestConfig {
        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
