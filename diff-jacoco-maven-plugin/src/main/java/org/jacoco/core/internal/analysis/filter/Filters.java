package org.jacoco.core.internal.analysis.filter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.objectweb.asm.tree.MethodNode;

/**
 * 插件对 jacoco 0.8.15 官方 {@code Filters} 的 shadow 副本（同包同类名，
 * 借助插件类加载器优先加载插件自身 jar 的机制遮蔽 jacoco core 中的同名类）。
 *
 * <p>相比官方实现，本副本唯一的增强是 {@link #addFilter(IFilter)} 注册表：
 * 自定义 filter（DiffFilter / PersonFilter 等）在 {@code Analyzer} 调用
 * {@code all()} 前注册进来，随内置 filter 一起生效；{@code all()} 每次调用
 * 都会重新读取注册表，因此注入无需再通过反射修改私有静态字段。</p>
 *
 * <p><b>版本锁定：内置 filter 清单（allCommonFilters / allNonKotlinFilters /
 * allKotlinFilters）必须与 pom 中 jacoco 依赖版本逐一核对同步，升级 jacoco
 * 时若官方清单有变，必须同步修改本类，否则官方新增的内置 filter 会被本类
 * 整体旁路。</b>清单同步自 v0.8.15 tag 的 Filters.java。</p>
 */
public final class Filters {

	/**
	 * Descriptor of annotation present in all class files produced by the
	 * Kotlin compiler.
	 */
	public static final String KOTLIN_METADATA_DESC = "Lkotlin/Metadata;";

	/**
	 * 自定义 filter 注册表。静态且线程安全，注册一次即对本插件 realm 内
	 * 后续所有分析与跨模块构建生效（Mojo 侧有 DIFF_FILTER_INJECTED
	 * 守卫，避免重复注册）。注册表读取发生在 {@link #all()} 调用时，
	 * 因此对已注册 mojo 的分析阶段天然可见。
	 */
	private static final List<IFilter> EXTRA_FILTERS = new CopyOnWriteArrayList<>();

	private Filters() {
		// no instances
	}

	/**
	 * Filter that does nothing.
	 */
	public static final IFilter NONE = new FilterSet();

	/**
	 * 注册自定义 filter，会在之后所有的 {@link #all()} 组装中被包含。
	 *
	 * @param filter 不可为 null 的自定义 filter
	 */
	public static void addFilter(final IFilter filter) {
		if (filter == null) {
			throw new IllegalArgumentException("filter must not be null");
		}
		EXTRA_FILTERS.add(filter);
	}

	/**
	 * Creates a filter that combines all filters.
	 *
	 * @return filter that combines all filters
	 */
	public static IFilter all() {
		final IFilter allCommonFilters = allCommonFilters();
		final IFilter allKotlinFilters = allKotlinFilters();
		final IFilter allNonKotlinFilters = allNonKotlinFilters();
		final IFilter allExtraFilters = new FilterSet(
				EXTRA_FILTERS.toArray(new IFilter[0]));
		return new IFilter() {
			public void filter(final MethodNode methodNode,
					final IFilterContext context, final IFilterOutput output) {
				allCommonFilters.filter(methodNode, context, output);
				if (isKotlinClass(context)) {
					allKotlinFilters.filter(methodNode, context, output);
				} else {
					allNonKotlinFilters.filter(methodNode, context, output);
				}
				allExtraFilters.filter(methodNode, context, output);
			}
		};
	}

	private static IFilter allCommonFilters() {
		return new FilterSet( //
				new SyntheticClassFilter(), //
				new EnumFilter(), //
				new BridgeFilter(), //
				new SynchronizedFilter(), //
				new TryWithResourcesJavac11Filter(), //
				new TryWithResourcesJavacFilter(), //
				new TryWithResourcesEcjFilter(), //
				new FinallyFilter(), //
				new PrivateEmptyNoArgConstructorFilter(), //
				new AssertFilter(), //
				new StringSwitchJavacFilter(), //
				new StringSwitchFilter(), //
				new EnumEmptyConstructorFilter(), //
				new RecordsFilter(), //
				new ExhaustiveSwitchFilter(), //
				new RecordPatternFilter(), //
				new AnnotationGeneratedFilter());
	}

	private static IFilter allNonKotlinFilters() {
		return new FilterSet( //
				new SyntheticFilter());
	}

	private static IFilter allKotlinFilters() {
		return new FilterSet( //
				new KotlinGeneratedFilter(), //
				new KotlinSyntheticAccessorsFilter(), //
				new KotlinSerializableFilter(), //
				new KotlinEnumFilter(), //
				new KotlinJvmOverloadsFilter(), //
				new KotlinJvmStaticFilter(), //
				new KotlinSafeCallOperatorFilter(), //
				new KotlinLateinitFilter(), //
				new KotlinWhenFilter(), //
				new KotlinWhenStringFilter(), //
				new KotlinUnsafeCastOperatorFilter(), //
				new KotlinNotNullOperatorFilter(), //
				new KotlinInlineClassFilter(), //
				new KotlinExposeBoxedFilter(), //
				new KotlinDefaultArgumentsFilter(), //
				new KotlinInlineFilter(), //
				new KotlinCoroutineFilter(), //
				new KotlinDefaultMethodsFilter(), //
				new KotlinComposeFilter());
	}

	/**
	 * Returns {@code true} if the class corresponding to this context has
	 * {@link #KOTLIN_METADATA_DESC kotlin.Metadata} annotation.
	 *
	 * @param context
	 *            context information
	 * @return {@code true} if the class corresponding to this context has
	 *         {@link #KOTLIN_METADATA_DESC kotlin.Metadata} annotation
	 */
	private static boolean isKotlinClass(final IFilterContext context) {
		return context.getClassAnnotations().contains(KOTLIN_METADATA_DESC);
	}

}
