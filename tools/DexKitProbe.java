import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class DexKitProbe {
    private static final List<String> FEED_STRINGS = List.of(
        "Sponsored",
        "sponsored",
        "SPONSORED",
        "Promoted",
        "promoted",
        "AdChoices",
        "adchoices",
        "sponsored_label",
        "sponsored_card",
        "sponsored_content",
        "ad_label",
        "ad_attribution",
        "ad_badge",
        "ad_choices",
        "promoted_card",
        "promoted_content",
        "DiscoverCard",
        "FeedCard",
        "StreamCard",
        "ContentCard",
        "MonetizableCard",
        "TaggedCard",
        "NativeAd"
    );

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: DexKitProbe <apk>");
            System.exit(2);
        }

        System.load(args0("DEXKIT_SO"));

        try (DexKitBridge bridge = DexKitBridge.create(args[0])) {
            dumpClassesUsingFeedStrings(bridge);
            dumpAdapterSubclasses(bridge, "android.support.v7.widget.RecyclerView$Adapter");
            dumpAdapterSubclasses(bridge, "androidx.recyclerview.widget.RecyclerView$Adapter");
            dumpDiscoverPackageListMethods(bridge);
        }
    }

    private static String args0(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing env var: " + name);
        }
        return value;
    }

    private static void dumpClassesUsingFeedStrings(DexKitBridge bridge) {
        System.out.println("== classes using feed strings ==");
        ClassMatcher matcher = ClassMatcher.create().usingEqStrings(FEED_STRINGS);
        List<ClassData> classes = bridge.findClass(FindClass.create().matcher(matcher));
        classes.stream()
            .sorted(Comparator.comparing(ClassData::getName))
            .forEach(DexKitProbe::printClassSummary);
    }

    private static void dumpAdapterSubclasses(DexKitBridge bridge, String superClass) {
        System.out.println("== adapter subclasses: " + superClass + " ==");
        ClassMatcher matcher = ClassMatcher.create().superClass(superClass);
        List<ClassData> classes = bridge.findClass(FindClass.create().matcher(matcher));
        classes.stream()
            .sorted(Comparator.comparing(ClassData::getName))
            .limit(80)
            .forEach(cls -> {
                System.out.println(cls.getName());
                cls.getMethods().stream()
                    .filter(method -> method.getMethodName().equals("onBindViewHolder"))
                    .forEach(method -> System.out.println("  bind " + method.getMethodSign()));
            });
        System.out.println("count=" + classes.size());
    }

    private static void dumpDiscoverPackageListMethods(DexKitBridge bridge) {
        System.out.println("== discover package list-ish methods ==");
        List<ClassData> classes =
            bridge.findClass(FindClass.create().searchPackages("com.google.android.apps.search.googleapp.discover"));
        classes.stream()
            .sorted(Comparator.comparing(ClassData::getName))
            .forEach(cls -> cls.getMethods().stream()
                .filter(method -> isListy(method.getReturnTypeName()) || looksLikeAccessor(method.getMethodName()))
                .forEach(method -> {
                    System.out.println(
                        cls.getName() + " :: " + method.getMethodSign() +
                            " strings=" + preview(method.getUsingStrings())
                    );
                }));
        System.out.println("discover-class-count=" + classes.size());
    }

    private static boolean isListy(String returnType) {
        return returnType.equals("java.util.List") ||
            returnType.equals("java.util.Collection") ||
            returnType.endsWith("List") ||
            returnType.endsWith("$List") ||
            returnType.contains("ImmutableList");
    }

    private static boolean looksLikeAccessor(String name) {
        return Arrays.asList("getCards", "cards", "getItems", "items", "getFeed", "feed", "getContent",
                "content", "getEntries", "entries").stream()
            .anyMatch(name::equalsIgnoreCase);
    }

    private static void printClassSummary(ClassData cls) {
        System.out.println(cls.getName());
        cls.getMethods().stream()
            .filter(method -> isListy(method.getReturnTypeName()) || looksLikeAccessor(method.getMethodName()))
            .forEach(method -> System.out.println(
                "  " + method.getMethodSign() + " strings=" + preview(method.getUsingStrings())
            ));
    }

    private static String preview(List<String> strings) {
        if (strings == null || strings.isEmpty()) return "[]";
        return strings.stream().limit(8).toList().toString();
    }
}
