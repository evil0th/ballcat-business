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

package org.ballcat.business.system.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.RequiredArgsConstructor;
import org.ballcat.business.system.converter.SysOrganizationConverter;
import org.ballcat.business.system.mapper.SysOrganizationMapper;
import org.ballcat.business.system.model.dto.OrganizationMoveChildParam;
import org.ballcat.business.system.model.dto.SysOrganizationDTO;
import org.ballcat.business.system.model.entity.SysOrganization;
import org.ballcat.business.system.model.qo.SysOrganizationQO;
import org.ballcat.business.system.model.vo.SysOrganizationTree;
import org.ballcat.business.system.service.SysOrganizationService;
import org.ballcat.business.system.service.SysUserService;
import org.ballcat.common.core.constant.GlobalConstants;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.common.util.TreeUtils;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 组织架构
 *
 * @author hccake 2020-09-23 12:09:43
 */
@Service
@RequiredArgsConstructor
public class SysOrganizationServiceImpl extends ExtendServiceImpl<SysOrganizationMapper, SysOrganization>
		implements SysOrganizationService {

	private final SysUserService sysUserService;

	/**
	 * 返回组织架构的树形结构
	 * @param sysOrganizationQO 组织机构查询条件
	 * @return OrganizationTree
	 */
	@Override
	public List<SysOrganizationTree> listTree(SysOrganizationQO sysOrganizationQO) {
		List<SysOrganization> list = this.list();
		List<SysOrganizationTree> tree = TreeUtils.buildTree(list, GlobalConstants.TREE_ROOT_ID_LONG,
				SysOrganizationConverter.INSTANCE::poToTree);

		// 如果有名称的查询条件，则进行剪枝操作
		String name = sysOrganizationQO.getName();
		if (StringUtils.hasText(name)) {
			return TreeUtils.pruneTree(tree, node -> node.getName() != null && node.getName().contains(name));
		}

		return tree;
	}

	/**
	 * 创建一个新的组织机构
	 * @param sysOrganizationDTO 组织机构DTO
	 * @return boolean 创建成功/失败
	 */
	@Override
	public boolean create(SysOrganizationDTO sysOrganizationDTO) {
		sysOrganizationDTO.setId(null);
		SysOrganization sysOrganization = SysOrganizationConverter.INSTANCE.dtoToPo(sysOrganizationDTO);

		// 如果父级为根节点则直接设置深度和层级，否则根据父节点数据动态设置
		Long parentId = sysOrganizationDTO.getParentId();
		// 填充层级和深度
		fillDepthAndHierarchy(sysOrganization, parentId);

		return SqlHelper.retBool(baseMapper.insert(sysOrganization));
	}

	/**
	 * 更新一个已有的组织机构
	 * @param sysOrganizationDTO 组织机构DTO
	 * @return boolean 更新成功/失败
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean update(SysOrganizationDTO sysOrganizationDTO) {
		// TODO 防止并发问题
		SysOrganization newSysOrganization = SysOrganizationConverter.INSTANCE.dtoToPo(sysOrganizationDTO);
		Long organizationId = newSysOrganization.getId();
		SysOrganization originSysOrganization = baseMapper.selectById(organizationId);

		// 如果没有移动父节点，则直接更新
		Long targetParentId = sysOrganizationDTO.getParentId();
		if (originSysOrganization.getParentId().equals(targetParentId)) {
			return SqlHelper.retBool(baseMapper.updateById(newSysOrganization));
		}

		// 移动了父节点，先判断不是选择自己作为父节点
		Assert.isTrue(!targetParentId.equals(organizationId), "父节点不能是自己！");
		// 再判断是否是自己的子节点，根节点跳过判断
		if (!GlobalConstants.TREE_ROOT_ID_LONG.equals(targetParentId)) {
			SysOrganization targetParentOrganization = baseMapper.selectById(targetParentId);
			String[] targetParentHierarchy = targetParentOrganization.getHierarchy().split("-");
			if (Arrays.asList(targetParentHierarchy).contains(String.valueOf(organizationId))) {
				throw new BusinessException(BaseResultCode.LOGIC_CHECK_ERROR.getCode(), "父节点不能是自己的子节点！");
			}
		}

		// 填充目标层级和深度
		fillDepthAndHierarchy(newSysOrganization, targetParentId);
		// 更新其子节点的数据
		OrganizationMoveChildParam param = getOrganizationMoveChildParam(newSysOrganization, originSysOrganization);
		baseMapper.followMoveChildNode(param);
		// 更新组织节点信息
		return SqlHelper.retBool(baseMapper.updateById(newSysOrganization));
	}

	private OrganizationMoveChildParam getOrganizationMoveChildParam(SysOrganization newSysOrganization,
			SysOrganization originSysOrganization) {
		// 父组织 id
		Long parentId = newSysOrganization.getId();
		// 父节点原来的层级
		String originParentHierarchy = originSysOrganization.getHierarchy();
		// 修改后的父节点层级
		String targetParentHierarchy = newSysOrganization.getHierarchy();
		// 父节点移动后的深度差
		int depthDiff = originSysOrganization.getDepth() - newSysOrganization.getDepth();

		OrganizationMoveChildParam param = new OrganizationMoveChildParam();
		param.setParentId(parentId);
		param.setOriginParentHierarchy(originParentHierarchy);
		param.setOriginParentHierarchyLengthPlusOne(originParentHierarchy.length() + 1);
		param.setTargetParentHierarchy(targetParentHierarchy);
		param.setDepthDiff(depthDiff);
		param.setGrandsonConditionalStatement(originParentHierarchy + "-" + parentId + "-%");
		return param;
	}

	/**
	 * 根据组织ID 查询除该组织下的所有儿子组织
	 * @param organizationId 组织机构ID
	 * @return List<SysOrganization> 该组织的儿子组织
	 */
	@Override
	public List<SysOrganization> listSubOrganization(Long organizationId) {
		return baseMapper.listSubOrganization(organizationId);
	}

	/**
	 * 根据组织ID 查询除该组织下的所有孩子（子孙）组织
	 * @param organizationId 组织机构ID
	 * @return List<SysOrganization> 该组织的孩子组织
	 */
	@Override
	public List<SysOrganization> listChildOrganization(Long organizationId) {
		return baseMapper.listChildOrganization(organizationId);
	}

	/**
	 * 校正组织机构层级和深度
	 * @return 校正是否成功
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean revisedHierarchyAndPath() {
		// 组织机构一般数据量不多，一次性查询出来缓存到内存中，减少查询开销
		List<SysOrganization> sysOrganizations = baseMapper.selectList(Wrappers.emptyWrapper());
		Map<Long, List<SysOrganization>> map = sysOrganizations.stream()
			.collect(Collectors.groupingBy(SysOrganization::getParentId));
		// 默认的父节点为根节点，
		Long parentId = GlobalConstants.TREE_ROOT_ID_LONG;
		int depth = 1;
		String hierarchy = "0";
		updateChildHierarchyAndPath(map, parentId, depth, hierarchy);

		return true;
	}

	private void updateChildHierarchyAndPath(Map<Long, List<SysOrganization>> map, Long parentId, int depth,
			String hierarchy) {
		// 获取对应 parentId 下的所有子节点
		List<SysOrganization> sysOrganizations = map.get(parentId);
		if (CollectionUtils.isEmpty(sysOrganizations)) {
			return;
		}
		// 递归更新子节点数据
		List<Long> childrenIds = new ArrayList<>();
		for (SysOrganization sysOrganization : sysOrganizations) {
			Long organizationId = sysOrganization.getId();
			updateChildHierarchyAndPath(map, organizationId, depth + 1, hierarchy + "-" + organizationId);
			childrenIds.add(organizationId);
		}
		baseMapper.updateHierarchyAndPathBatch(depth, hierarchy, childrenIds);
	}

	/**
	 * 根据组织ID 删除组织机构
	 * @param id 组织机构ID
	 * @return 删除成功: true
	 */
	@Override
	public boolean removeById(Serializable id) {
		Long organizationId = (Long) id;
		Boolean existsChildOrganization = baseMapper.existsChildOrganization(organizationId);
		if (Boolean.TRUE.equals(existsChildOrganization)) {
			throw new BusinessException(BaseResultCode.LOGIC_CHECK_ERROR.getCode(), "该组织机构拥有下级组织，不能删除！");
		}
		if (this.sysUserService.existsForOrganization(organizationId)) {
			throw new BusinessException(BaseResultCode.LOGIC_CHECK_ERROR.getCode(), "该组织机构拥有关联用户，不能删除！");
		}
		return SqlHelper.retBool(baseMapper.deleteById(id));
	}

	/**
	 * 根据父级ID填充当前组织机构实体的深度和层级
	 * @param sysOrganization 组织机构实体
	 * @param parentId 父级ID
	 */
	private void fillDepthAndHierarchy(SysOrganization sysOrganization, Long parentId) {
		if (GlobalConstants.TREE_ROOT_ID_LONG.equals(parentId)) {
			sysOrganization.setDepth(1);
			sysOrganization.setHierarchy(GlobalConstants.TREE_ROOT_ID_LONG.toString());
		}
		else {
			SysOrganization parentSysOrganization = baseMapper.selectById(parentId);
			Assert.notNull(parentSysOrganization, "不存在的父级组织机构！");

			sysOrganization.setDepth(parentSysOrganization.getDepth() + 1);
			sysOrganization.setHierarchy(parentSysOrganization.getHierarchy() + "-" + parentSysOrganization.getId());
		}
	}

}
