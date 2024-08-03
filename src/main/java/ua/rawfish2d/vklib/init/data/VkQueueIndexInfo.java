package ua.rawfish2d.vklib.init.data;

import java.util.List;

public record VkQueueIndexInfo(int queueIndex, boolean presentSupport, int queueType) {
	public static VkQueueIndexInfo getByType(List<VkQueueIndexInfo> queueInfoList, int queueType) {
		for (VkQueueIndexInfo queueInfo : queueInfoList) {
			if ((queueInfo.queueType & queueType) == queueType) {
				return queueInfo;
			}
		}
		return null;
	}

	public static VkQueueIndexInfo getWithPresent(List<VkQueueIndexInfo> queueInfoList, int queueType) {
		for (VkQueueIndexInfo queueInfo : queueInfoList) {
			if ((queueInfo.queueType & queueType) == queueType && queueInfo.presentSupport) {
				return queueInfo;
			}
		}
		return null;
	}

	public static VkQueueIndexInfo getWithPresent(List<VkQueueIndexInfo> queueInfoList) {
		for (VkQueueIndexInfo queueInfo : queueInfoList) {
			if (queueInfo.presentSupport) {
				return queueInfo;
			}
		}
		return null;
	}

	public static VkQueueIndexInfo getByIndex(List<VkQueueIndexInfo> queueInfoList, int index) {
		for (VkQueueIndexInfo queueInfo : queueInfoList) {
			if (queueInfo.queueIndex == index) {
				return queueInfo;
			}
		}
		return null;
	}
}