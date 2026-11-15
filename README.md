# E4-08 管理员服务与缓存服务循环依赖的替换依赖改造

学号 20244350112（姓名已做隐私化处理）

## 作品文件

| 文件 | 说明 |
|---|---|
| `E4-一页说明-20244350112.md` | 一页说明（讲清楚/有依据/验证过） |
| `E4-发言稿-20244350112.md` | 2 分钟录屏发言稿 |
| `src/test/java/com/macro/mall/UmsAdminCacheServiceImplTest.java` | 改后测试（4 用例，含 Spring 装配验证） |
| `src/main/java/com/macro/mall/service/impl/UmsAdminCacheServiceImpl.java` | 改后产品代码（adminService 构造注入） |
| `evidence/1_test_after.log` | 改后测试输出（Tests run: 4, Failures: 0） |
| `evidence/2_coverage.log` | JaCoCo 覆盖率生成日志 |
| `run_e4-08.bat` | 一键运行脚本 |

## 证据链接

- 改后测试绿灯：`evidence/1_test_after.log`
- 覆盖率报告：`evidence/2_coverage.log`

## 改造摘要

把 `UmsAdminCacheServiceImpl` 的 `adminService` 字段注入（`@Autowired`）改为构造注入，
消除循环依赖一端，使单元测试可直接 `new UmsAdminCacheServiceImpl(mockAdminService)` 传替身。
其余字段（redisService、mapper、dao）保持字段注入不变。详见一页说明。
