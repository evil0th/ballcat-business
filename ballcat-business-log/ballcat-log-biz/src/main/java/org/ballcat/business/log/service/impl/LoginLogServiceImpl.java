/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballcat.business.log.service.impl;

import org.ballcat.business.log.mapper.LoginLogMapper;
import org.ballcat.business.log.model.entity.LoginLog;
import org.ballcat.business.log.model.qo.LoginLogQO;
import org.ballcat.business.log.model.vo.LoginLogPageVO;
import org.ballcat.business.log.service.LoginLogService;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 登录日志
 *
 * @author hccake 2020-09-16 20:21:10
 */
@Service
public class LoginLogServiceImpl extends ExtendServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

	/**
	 * 根据QueryObject查询分页数据
	 * @param pageParam 分页参数
	 * @param qo 查询参数对象
	 * @return PageResult<LoginLogVO> 分页数据
	 */
	@Override
	public PageResult<LoginLogPageVO> queryPage(PageParam pageParam, LoginLogQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

}
