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

package org.ballcat.business.i18n.model.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 国际化信息传输对象
 *
 * @author hccake 2021-08-06 10:48:25
 */
@Data
@Schema(title = "国际化信息传输对象")
public class I18nDataCreateDTO {

	private static final long serialVersionUID = 1L;

	/**
	 * 唯一标识 = 业务:关键词
	 */
	@NotEmpty(message = "{i18nMessage.code}：{}")
	@Schema(title = "唯一标识 = 业务:关键词")
	private String code;

	/**
	 * 语言文本列表
	 */
	@Valid
	@NotNull(message = "{i18nData.languageTexts}: {}")
	@Size(min = 1, message = "{i18nData.languageTexts}: {}")
	@Schema(title = "语言文本列表")
	private List<LanguageText> languageTexts;

	/**
	 * 备注
	 */
	@Schema(title = "备注")
	private String remarks;

	/**
	 * 语言文本
	 */
	@Data
	@Schema(title = "语言文本信息")
	public static class LanguageText {

		/**
		 * 语言标签
		 */
		@NotEmpty(message = "{i18nMessage.languageTag}：{}")
		@Schema(title = "语言标签")
		private String languageTag;

		/**
		 * 文本值，可以使用 { } 加角标，作为占位符
		 */
		@NotEmpty(message = "{i18nMessage.message}：{}")
		@Schema(title = "文本值，可以使用 { } 加角标，作为占位符")
		private String message;

	}

}
