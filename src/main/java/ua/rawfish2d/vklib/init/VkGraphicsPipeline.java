package ua.rawfish2d.vklib.init;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ua.rawfish2d.vklib.attrib.AttribFormat;
import ua.rawfish2d.vklib.init.data.ShaderStage;
import ua.rawfish2d.vklib.init.descriptor.VkDescriptorSetLayout;
import ua.rawfish2d.vklib.utils.VkHelper;
import ua.rawfish2d.vklib.utils.VkTranslate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class VkGraphicsPipeline {
	@Getter
	private long vkPipelineLayout;
	private long vkGraphicsPipeline;
	private final List<ShaderStage> shaderStagesInfoList = new ArrayList<>();
	private AttribFormat attribFormat;
	private int topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
	private boolean primitiveRestart = false;
	private int polygonMode = VK_POLYGON_MODE_FILL;
	private float lineWidth = 1f;
	private int cullMode = VK_CULL_MODE_FRONT_BIT;
	private int winding = VK_FRONT_FACE_COUNTER_CLOCKWISE;
	private boolean blending = false;
	private int srcColorBlendFactor = VK_BLEND_FACTOR_SRC_ALPHA;
	private int dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
	private int colorBlendOp = VK_BLEND_OP_ADD;
	private int srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
	private int dstAlphaBlendFactor = VK_BLEND_FACTOR_ZERO;
	private int alphaBlendOp = VK_BLEND_OP_ADD;
	@Getter
	private VkDescriptorSetLayout vkDescriptorSetLayout;

	public VkGraphicsPipeline() {
	}

	public VkGraphicsPipeline addVertexShader(String shaderPath) {
		shaderStagesInfoList.add(new ShaderStage(VK_SHADER_STAGE_VERTEX_BIT, "main", shaderPath));
		return this;
	}

	public VkGraphicsPipeline addFragmentShader(String shaderPath) {
		shaderStagesInfoList.add(new ShaderStage(VK_SHADER_STAGE_FRAGMENT_BIT, "main", shaderPath));
		return this;
	}

	public VkGraphicsPipeline attribFormat(AttribFormat attribFormat) {
		this.attribFormat = attribFormat;
		return this;
	}

	/**
	 * VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST
	 * VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP
	 * VK_PRIMITIVE_TOPOLOGY_LINE_LIST
	 * VK_PRIMITIVE_TOPOLOGY_LINE_STRIP
	 * etc
	 */
	public VkGraphicsPipeline topology(int topology) {
		this.topology = topology;
		return this;
	}

	public VkGraphicsPipeline primitiveRestart(boolean primitiveRestart) {
		this.primitiveRestart = primitiveRestart;
		return this;
	}

	/**
	 * VK_POLYGON_MODE_FILL, VK_POLYGON_MODE_LINE, VK_POLYGON_MODE_POINT
	 */
//	public VkGraphicsPipeline polygonMode(int polygonMode) {
//		this.polygonMode = polygonMode;
//		return this;
//	}
	public VkGraphicsPipeline polygonModeFill() {
		this.polygonMode = VK_POLYGON_MODE_FILL;
		return this;
	}

	public VkGraphicsPipeline polygonModeLine() {
		this.polygonMode = VK_POLYGON_MODE_LINE;
		return this;
	}

	public VkGraphicsPipeline polygonModePoint() {
		this.polygonMode = VK_POLYGON_MODE_POINT;
		return this;
	}

	public VkGraphicsPipeline lineWidth(float lineWidth) {
		this.lineWidth = lineWidth;
		return this;
	}

	/**
	 * VK_CULL_MODE_NONE, VK_CULL_MODE_FRONT_BIT, VK_CULL_MODE_BACK_BIT, VK_CULL_MODE_FRONT_AND_BACK
	 */
//	public VkGraphicsPipeline cullMode(int cullMode) {
//		this.cullMode = cullMode;
//		return this;
//	}
	public VkGraphicsPipeline cullModeNone() {
		this.cullMode = VK_CULL_MODE_NONE;
		return this;
	}

	public VkGraphicsPipeline cullModeFront() {
		this.cullMode = VK_CULL_MODE_FRONT_BIT;
		return this;
	}

	public VkGraphicsPipeline cullModeBack() {
		this.cullMode = VK_CULL_MODE_BACK_BIT;
		return this;
	}

	public VkGraphicsPipeline cullModeFrontAndBack() {
		this.cullMode = VK_CULL_MODE_FRONT_AND_BACK;
		return this;
	}

	/**
	 * VK_FRONT_FACE_COUNTER_CLOCKWISE or VK_FRONT_FACE_CLOCKWISE
	 */
//	public VkGraphicsPipeline winding(int winding) {
//		this.winding = winding;
//		return this;
//	}
	public VkGraphicsPipeline windingCounterClockwise() {
		this.winding = VK_FRONT_FACE_COUNTER_CLOCKWISE;
		return this;
	}

	public VkGraphicsPipeline windingClockwise() {
		this.winding = VK_FRONT_FACE_CLOCKWISE;
		return this;
	}

	public VkGraphicsPipeline blending(boolean blending) {
		this.blending = blending;
		return this;
	}

	public VkGraphicsPipeline blendFunc(int srcColorBlendFactor, int dstColorBlendFactor) {
		this.srcColorBlendFactor = srcColorBlendFactor;
		this.dstColorBlendFactor = dstColorBlendFactor;
		return this;
	}

	public VkGraphicsPipeline setDescriptorSetLayout(VkDescriptorSetLayout vkDescriptorSetLayout) {
		this.vkDescriptorSetLayout = vkDescriptorSetLayout;
		return this;
	}

	public VkGraphicsPipeline create(VkDeviceInstance deviceInstance) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkPipelineShaderStageCreateInfo.Buffer vkShaderStagesBuffer = VkPipelineShaderStageCreateInfo.calloc(shaderStagesInfoList.size(), stack);
			final List<Long> shaderModules = new ArrayList<>();
			for (ShaderStage stage : shaderStagesInfoList) {
				final long vkShaderModule;
				try {
					vkShaderModule = createShaderModule(deviceInstance.getVkLogicalDevice(), stage.shaderPath(), stage.stageType(), stack);
				} catch (IOException e) {
					e.printStackTrace();
					// RIPBOZO
					throw new RuntimeException("Exception: " + e);
				}
				final VkPipelineShaderStageCreateInfo vkShadeStageCreateInfo = VkPipelineShaderStageCreateInfo.calloc(stack)
						.sType$Default()
						.stage(stage.stageType())
						.module(vkShaderModule)
						.pName(stack.UTF8(stage.entryPointName()))
						.pSpecializationInfo(null);
				vkShaderStagesBuffer.put(vkShadeStageCreateInfo);
				shaderModules.add(vkShaderModule);
			}
			vkShaderStagesBuffer.flip();

			// ==========

			final IntBuffer dynamicStates = stack.mallocInt(2);
			dynamicStates.put(0, VK_DYNAMIC_STATE_VIEWPORT);
			dynamicStates.put(1, VK_DYNAMIC_STATE_SCISSOR);
			final VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
					.sType$Default()
					.pDynamicStates(dynamicStates);

			// vertex format similar to setting up VAO/VBO formats
			final VkPipelineVertexInputStateCreateInfo vertexInputInfo = attribFormat.makeVertexAttribDescription(stack);

			final VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
					.sType$Default()
					.topology(topology)
					.primitiveRestartEnable(primitiveRestart);

			final VkExtent2D vkExtent2D = deviceInstance.getVkExtent2D();
			final VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
			viewport.x(0.0f).y(0.0f);
			viewport.width(vkExtent2D.width()).height(vkExtent2D.height());
			viewport.minDepth(0f).maxDepth(1f);

			final VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
			scissor.offset().set(0, 0);
			scissor.extent(vkExtent2D);

			final VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
					.sType$Default()
					.pViewports(viewport)
					.viewportCount(1)
					.pScissors(scissor)
					.scissorCount(1);

			// ========== CULLING ==========

			final VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
					.sType$Default()
					.depthClampEnable(false)
					.polygonMode(polygonMode)
					.lineWidth(lineWidth)
					.cullMode(cullMode)
					.frontFace(winding)
					.depthBiasEnable(false)
					.depthBiasConstantFactor(0f)
					.depthBiasClamp(0f)
					.depthBiasSlopeFactor(0f);

			// ========== MULTISAMPLING ==========

			final VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
					.sType$Default()
					.sampleShadingEnable(false)
					.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
					.minSampleShading(1.0f) // Optional
					.pSampleMask(null) // Optional
					.alphaToCoverageEnable(false) // Optional
					.alphaToOneEnable(false); // Optional

			// ========== DEPTH STENCIL ==========

			// TODO
			final VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
					.sType$Default()
					.depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
					.depthTestEnable(false)
					.depthWriteEnable(false)
					.minDepthBounds(0f)
					.maxDepthBounds(1f)
					.depthBoundsTestEnable(false)
					.stencilTestEnable(false);

			// ========== BLENDING ==========

			// per framebuffer blending
			final VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
					.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
					// no blending
					.blendEnable(blending)
					.srcColorBlendFactor(srcColorBlendFactor) // Optional
					.dstColorBlendFactor(dstColorBlendFactor) // Optional
					.colorBlendOp(colorBlendOp) // Optional
					.srcAlphaBlendFactor(srcAlphaBlendFactor) // Optional
					.dstAlphaBlendFactor(dstAlphaBlendFactor) // Optional
					.alphaBlendOp(alphaBlendOp); // Optional
			// alpha blending
			// glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//					.blendEnable(true)
//					.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA) // Optional
//					.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA) // Optional
//					.colorBlendOp(VK_BLEND_OP_ADD) // Optional
//					.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE) // Optional
//					.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO) // Optional
//					.alphaBlendOp(VK_BLEND_OP_ADD); // Optional
			// blending
			// glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//					.blendEnable(true)
//					.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA) // Optional
//					.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA) // Optional
//					.colorBlendOp(VK_BLEND_OP_ADD) // Optional
//					.srcAlphaBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA) // Optional
//					.dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA) // Optional
//					.alphaBlendOp(VK_BLEND_OP_ADD); // Optional

			// global blending
			final VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
					.sType$Default()
					.logicOpEnable(false)
					.logicOp(VK_LOGIC_OP_COPY) // Optional
					.attachmentCount(1)
					.pAttachments(colorBlendAttachment);
			colorBlending.blendConstants().put(0, 0f); // Optional
			colorBlending.blendConstants().put(1, 0f); // Optional
			colorBlending.blendConstants().put(2, 0f); // Optional
			colorBlending.blendConstants().put(3, 0f); // Optional

			// ========== PIPELINE LAYOUT ==========

			final VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
					.sType$Default()
					.setLayoutCount(1)
					.pSetLayouts(stack.longs(vkDescriptorSetLayout.getHandle()))
					.pPushConstantRanges(null); // Optional

			final LongBuffer pPipelineLayout = stack.mallocLong(1);
			if (vkCreatePipelineLayout(deviceInstance.getVkLogicalDevice(), pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create pipeline layout!");
			}
			vkPipelineLayout = pPipelineLayout.get(0);

			// ========== LAYOUT ==========

			// finally
			final VkGraphicsPipelineCreateInfo.Buffer pipelineInfos = VkGraphicsPipelineCreateInfo.calloc(1, stack)
					.sType$Default()
					.stageCount(shaderStagesInfoList.size())
					.pStages(vkShaderStagesBuffer)
					.pVertexInputState(vertexInputInfo)
					.pInputAssemblyState(inputAssembly)
					.pViewportState(viewportState)
					.pRasterizationState(rasterizer)
					.pMultisampleState(multisampling)
					.pDepthStencilState(depthStencil) // optional
					.pColorBlendState(colorBlending)
					.pDynamicState(dynamicState)
					.layout(vkPipelineLayout)
					.subpass(0)
					.basePipelineHandle(VK_NULL_HANDLE) // optional
					.basePipelineIndex(-1); // optional

//			if (useDynamicRendering) {
			final VkPipelineRenderingCreateInfoKHR vkPipelineRenderingCreateInfoKHR = VkPipelineRenderingCreateInfoKHR.calloc(stack)
					.sType$Default()
					.colorAttachmentCount(1)
					.pColorAttachmentFormats(stack.ints(deviceInstance.getVkSwapChainImageFormat()));

			// dynamic rendering
			pipelineInfos.renderPass(VK_NULL_HANDLE);
			pipelineInfos.pNext(vkPipelineRenderingCreateInfoKHR);
//			} else {
//				pipelineInfos.renderPass(vkRenderPass);
//			}

			final LongBuffer pGraphicsPipeline = stack.mallocLong(1);
			final int result = vkCreateGraphicsPipelines(deviceInstance.getVkLogicalDevice(), VK_NULL_HANDLE, pipelineInfos, null, pGraphicsPipeline);
			if (result != VK_SUCCESS) {
				throw new RuntimeException("Failed to create graphics pipeline! " + VkTranslate.translateVulkanResult(result));
			}
			vkGraphicsPipeline = pGraphicsPipeline.get(0);

			for (long shaderModule : shaderModules) {
				vkDestroyShaderModule(deviceInstance.getVkLogicalDevice(), shaderModule, null);
			}
			shaderStagesInfoList.clear();
			attribFormat = null;
		}
		return this;
	}

	public void createGraphicsPipeline(VkDevice vkLogicalDevice, VkExtent2D vkExtent2D, long vkRenderPass, int vkSwapChainImageFormat, AttribFormat attribFormat, long descriptorSetLayout, String vertShader, String fragShader) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final long vertShaderModule = createShaderModule(vkLogicalDevice, vertShader, VK_SHADER_STAGE_VERTEX_BIT, stack);
			final long fragShaderModule = createShaderModule(vkLogicalDevice, fragShader, VK_SHADER_STAGE_FRAGMENT_BIT, stack);

			final ByteBuffer pName = stack.UTF8("main");
			final VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

			shaderStages.put(0, VkPipelineShaderStageCreateInfo.calloc(stack)
					.sType$Default()
					.stage(VK_SHADER_STAGE_VERTEX_BIT)
					.module(vertShaderModule)
					.pName(pName)
					.pSpecializationInfo(null));

			shaderStages.put(1, VkPipelineShaderStageCreateInfo.calloc(stack)
					.sType$Default()
					.stage(VK_SHADER_STAGE_FRAGMENT_BIT)
					.module(fragShaderModule)
					.pName(pName)
					.pSpecializationInfo(null));

			final IntBuffer dynamicStates = stack.mallocInt(2);
			dynamicStates.put(0, VK_DYNAMIC_STATE_VIEWPORT);
			dynamicStates.put(1, VK_DYNAMIC_STATE_SCISSOR);
			final VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
					.sType$Default()
					.pDynamicStates(dynamicStates);

			// vertex format similar to setting up VAO
			final VkPipelineVertexInputStateCreateInfo vertexInputInfo = attribFormat.makeVertexAttribDescription(stack);

			final VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
					.sType$Default()
					.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
					.primitiveRestartEnable(false);

			final VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
			viewport.x(0.0f).y(0.0f);
			viewport.width(vkExtent2D.width()).height(vkExtent2D.height());
			viewport.minDepth(0f).maxDepth(1f);

			final VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
			scissor.offset().set(0, 0);
			scissor.extent(vkExtent2D);

			final VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
					.sType$Default()
					.pViewports(viewport)
					.viewportCount(1)
					.pScissors(scissor)
					.scissorCount(1);

			final VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
					.sType$Default()
					.depthClampEnable(false)
					.polygonMode(VK_POLYGON_MODE_FILL)
					.lineWidth(1.0f)
					.cullMode(VK_CULL_MODE_FRONT_BIT)
					.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
					.depthBiasEnable(false)
					.depthBiasConstantFactor(0f)
					.depthBiasClamp(0f)
					.depthBiasSlopeFactor(0f);

			final VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
					.sType$Default()
					.sampleShadingEnable(false)
					.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
					.minSampleShading(1.0f) // Optional
					.pSampleMask(null) // Optional
					.alphaToCoverageEnable(false) // Optional
					.alphaToOneEnable(false); // Optional

			final VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
					.sType$Default()
					.depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
					.depthTestEnable(false)
					.depthWriteEnable(false)
					.minDepthBounds(0f)
					.maxDepthBounds(1f)
					.depthBoundsTestEnable(false)
					.stencilTestEnable(false);

			// per framebuffer blending
			final VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
					.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
					// no blending
					.blendEnable(false)
					.srcColorBlendFactor(VK_BLEND_FACTOR_ONE) // Optional
					.dstColorBlendFactor(VK_BLEND_FACTOR_ZERO) // Optional
					.colorBlendOp(VK_BLEND_OP_ADD) // Optional
					.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE) // Optional
					.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO) // Optional
					.alphaBlendOp(VK_BLEND_OP_ADD); // Optional
			// alpha blending
			// glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//					.blendEnable(true)
//					.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA) // Optional
//					.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA) // Optional
//					.colorBlendOp(VK_BLEND_OP_ADD) // Optional
//					.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE) // Optional
//					.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO) // Optional
//					.alphaBlendOp(VK_BLEND_OP_ADD); // Optional
			// blending
			// glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//					.blendEnable(true)
//					.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA) // Optional
//					.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA) // Optional
//					.colorBlendOp(VK_BLEND_OP_ADD) // Optional
//					.srcAlphaBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA) // Optional
//					.dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA) // Optional
//					.alphaBlendOp(VK_BLEND_OP_ADD); // Optional

			// global blending
			final VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
					.sType$Default()
					.logicOpEnable(false)
					.logicOp(VK_LOGIC_OP_COPY) // Optional
					.attachmentCount(1)
					.pAttachments(colorBlendAttachment);
			colorBlending.blendConstants().put(0, 0f); // Optional
			colorBlending.blendConstants().put(1, 0f); // Optional
			colorBlending.blendConstants().put(2, 0f); // Optional
			colorBlending.blendConstants().put(3, 0f); // Optional

			final VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
					.sType$Default()
					.setLayoutCount(1)
					.pSetLayouts(stack.longs(descriptorSetLayout))
					.pPushConstantRanges(null); // Optional

			final LongBuffer pPipelineLayout = stack.mallocLong(1);
			if (vkCreatePipelineLayout(vkLogicalDevice, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create pipeline layout!");
			}
			vkPipelineLayout = pPipelineLayout.get(0);

			// finally
			final VkGraphicsPipelineCreateInfo.Buffer pipelineInfos = VkGraphicsPipelineCreateInfo.calloc(1, stack)
					.sType$Default()
					.stageCount(2)
					.pStages(shaderStages)
					.pVertexInputState(vertexInputInfo)
					.pInputAssemblyState(inputAssembly)
					.pViewportState(viewportState)
					.pRasterizationState(rasterizer)
					.pMultisampleState(multisampling)
					.pDepthStencilState(depthStencil) // optional
					.pColorBlendState(colorBlending)
					.pDynamicState(dynamicState)
					.layout(vkPipelineLayout)
					.subpass(0)
					.basePipelineHandle(VK_NULL_HANDLE) // optional
					.basePipelineIndex(-1); // optional

//			if (VK2D.useDynamicRendering) {
			final VkPipelineRenderingCreateInfoKHR vkPipelineRenderingCreateInfoKHR = VkPipelineRenderingCreateInfoKHR.calloc(stack)
					.sType$Default()
					.colorAttachmentCount(1)
					.pColorAttachmentFormats(stack.ints(vkSwapChainImageFormat));

			pipelineInfos.renderPass(VK_NULL_HANDLE);
			pipelineInfos.pNext(vkPipelineRenderingCreateInfoKHR);
//			} else {
//				pipelineInfos.renderPass(vkRenderPass);
//			}

			final LongBuffer pGraphicsPipeline = stack.mallocLong(1);
			final int result = vkCreateGraphicsPipelines(vkLogicalDevice, VK_NULL_HANDLE, pipelineInfos, null, pGraphicsPipeline);
			if (result != VK_SUCCESS) {
				throw new RuntimeException("Failed to create graphics pipeline! " + VkTranslate.translateVulkanResult(result));
			}
			vkGraphicsPipeline = pGraphicsPipeline.get(0);

			vkDestroyShaderModule(vkLogicalDevice, fragShaderModule, null);
			vkDestroyShaderModule(vkLogicalDevice, vertShaderModule, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private long createShaderModule(VkDevice vkLogicalDevice, String shaderPath, int stage, MemoryStack stack) throws IOException {
		final ByteBuffer shaderCode = VkHelper.glslToSpirv(shaderPath, stage);
		final VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
				.sType$Default()
				.pCode(shaderCode);

		final LongBuffer shaderModule = stack.mallocLong(1);
		if (vkCreateShaderModule(vkLogicalDevice, createInfo, null, shaderModule) != VK_SUCCESS) {
			throw new RuntimeException("Failed to create shader module! Shader path: " + shaderPath);
		}
		return shaderModule.get(0);
	}

	public void bindPipeline(VkCommandBuffer commandBuffer) {
		vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, vkGraphicsPipeline);
	}

	public void destroyPipelineAndLayout(VkDeviceInstance vkDeviceInstance) {
		final VkDevice vkLogicalDevice = vkDeviceInstance.getVkLogicalDevice();
		vkDestroyPipeline(vkLogicalDevice, vkGraphicsPipeline, null);
		vkDestroyPipelineLayout(vkLogicalDevice, vkPipelineLayout, null);
	}

	public void destroyDescriptorSetLayout(VkDeviceInstance vkDeviceInstance) {
		final VkDevice vkLogicalDevice = vkDeviceInstance.getVkLogicalDevice();
		vkDestroyDescriptorSetLayout(vkLogicalDevice, vkDescriptorSetLayout.getHandle(), null);
	}

	public void destroy(VkDeviceInstance vkDeviceInstance) {
		destroyPipelineAndLayout(vkDeviceInstance);
		destroyDescriptorSetLayout(vkDeviceInstance);
	}

//	public void createDescriptorPool(VkDeviceInstance vkDeviceInstance, final int descriptorPoolSize) {
////		final int descriptorPoolSize = descriptorSetLayout.calculateDescriptorPoolSize(uniformBuffersCount);
//		System.out.println("🔷 pool size: " + descriptorPoolSize);
//		try (MemoryStack stack = MemoryStack.stackPush()) {
//			final VkDescriptorPoolSize.Buffer vkDescriptorPoolSizes = vkDescriptorSetLayout.makeDescriptorPoolSizes(stack, descriptorPoolSize);
//
//			final VkDescriptorPoolCreateInfo poolCreateInfo = VkDescriptorPoolCreateInfo.calloc(stack)
//					.sType$Default()
//					.pPoolSizes(vkDescriptorPoolSizes)
//					.maxSets(descriptorPoolSize);
//
//			final LongBuffer pDescriptorPool = stack.mallocLong(1);
//			if (vkCreateDescriptorPool(vkDeviceInstance.getVkLogicalDevice(), poolCreateInfo, null, pDescriptorPool) != VK_SUCCESS) {
//				throw new RuntimeException("Failed to create descriptor pool!");
//			}
//			vkDescriptorPool = pDescriptorPool.get(0);
//		}
//	}
}
