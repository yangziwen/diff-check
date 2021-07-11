package org.jacoco.core.internal.analysis.filter;

import org.objectweb.asm.tree.MethodNode;

/**
 * Filter that combines other filters.
 */
public final class Filters implements IFilter {

	/**
	 * Filter that does nothing.
	 */
	public static final IFilter NONE = new Filters();

	public static final IFilter ALL = new Filters(new EnumFilter(), new SyntheticFilter(),
            new BridgeFilter(), new SynchronizedFilter(),
            new TryWithResourcesJavac11Filter(),
            new TryWithResourcesJavacFilter(),
            new TryWithResourcesEcjFilter(), new FinallyFilter(),
            new PrivateEmptyNoArgConstructorFilter(),
            new StringSwitchJavacFilter(), new StringSwitchFilter(),
            new EnumEmptyConstructorFilter(), new RecordsFilter(),
            new AnnotationGeneratedFilter(), new KotlinGeneratedFilter(),
            new KotlinLateinitFilter(), new KotlinWhenFilter(),
            new KotlinWhenStringFilter(),
            new KotlinUnsafeCastOperatorFilter(),
            new KotlinNotNullOperatorFilter(),
            new KotlinDefaultArgumentsFilter(), new KotlinInlineFilter(),
            new KotlinCoroutineFilter(), new KotlinDefaultMethodsFilter());

	private final IFilter[] filters;

	/**
	 * Creates filter that combines all other filters.
	 *
	 * @return filter that combines all other filters
	 */
	public static IFilter all() {
		return ALL;
	}

	private Filters(final IFilter... filters) {
		this.filters = filters;
	}

	@Override
    public void filter(final MethodNode methodNode,
			final IFilterContext context, final IFilterOutput output) {
		for (final IFilter filter : filters) {
			filter.filter(methodNode, context, output);
		}
	}

}
