package ua.rawfish2d.vklib.init.descriptor;

public record DescriptorSetUpdate(boolean image,
                                  int binding, int descriptorType,
                                  long imageView, long imageSampler,
                                  long bufferHandle, long bufferOffset, long bufferRange) {

	public static DescriptorSetUpdate image(int binding, int descriptorType, long imageView, long imageSampler) {
		return new DescriptorSetUpdate(true, binding, descriptorType, imageView, imageSampler, 0, 0, 0);
	}

	public static DescriptorSetUpdate buffer(int binding, int descriptorType, long bufferHandle, long bufferOffset, long bufferRange) {
		return new DescriptorSetUpdate(false, binding, descriptorType, 0, 0, bufferHandle, bufferOffset, bufferRange);
	}
}