package ua.rawfish2d.vklib.init.descriptor;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SetLayoutBindingsBuffer {
	private final List<SetLayoutBinding> setLayoutBindings = new ArrayList<>();

	public int entriesCount() {
		return setLayoutBindings.size();
	}

	public SetLayoutBindingsBuffer add(int binding, int descriptorType, int stageFlags) {
		setLayoutBindings.add(new SetLayoutBinding(binding, descriptorType, stageFlags));
		return this;
	}
}