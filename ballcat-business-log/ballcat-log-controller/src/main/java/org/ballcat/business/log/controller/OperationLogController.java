package org.ballcat.business.log.controller;

import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.R;
import org.ballcat.business.log.model.qo.OperationLogQO;
import org.ballcat.business.log.model.vo.OperationLogPageVO;
import org.ballcat.business.log.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.security.annotation.Authorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志
 *
 * @author hccake 2019-10-15 20:42:32
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/log/operation-log")
@Tag(name = "操作日志管理")
public class OperationLogController {

	private final OperationLogService operationLogService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param operationLogQO 操作日志
	 * @return R
	 */
	@GetMapping("/page")
	@Authorize("hasPermission('log:operation-log:read')")
	@Operation(summary = "分页查询", description = "分页查询")
	public R<PageResult<OperationLogPageVO>> getOperationLogAdminPage(@Validated PageParam pageParam,
			OperationLogQO operationLogQO) {
		return R.ok(operationLogService.queryPage(pageParam, operationLogQO));
	}

}
