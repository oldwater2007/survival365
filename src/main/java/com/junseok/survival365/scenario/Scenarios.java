package com.junseok.survival365.scenario;

import com.junseok.survival365.scenario.baghdad.BaghdadScenario;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Scenario registry. Registration order is the order shown on the create-world screen. */
public final class Scenarios {
	private static final Map<Identifier, Supplier<WorldScenario>> REGISTRY = new LinkedHashMap<>();

	public static final Identifier BAGHDAD = register(BaghdadScenario.ID, BaghdadScenario::new);

	private Scenarios() {
	}

	public static Identifier register(Identifier id, Supplier<WorldScenario> factory) {
		if (REGISTRY.putIfAbsent(id, factory) != null) {
			throw new IllegalArgumentException("Duplicate scenario id " + id);
		}
		return id;
	}

	public static List<Identifier> ids() {
		return List.copyOf(REGISTRY.keySet());
	}

	public static Identifier defaultId() {
		return BAGHDAD;
	}

	public static @Nullable WorldScenario create(@Nullable Identifier id) {
		Supplier<WorldScenario> factory = id == null ? null : REGISTRY.get(id);
		return factory == null ? null : factory.get();
	}

	/** Forces class init so the static registrations above run. */
	public static void bootstrap() {
	}
}
