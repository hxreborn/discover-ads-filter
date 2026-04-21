import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.io.PrintStream;

public final class DexKitProbe {
    private static final PrintStream OUT = System.out;
    private static final PrintStream ERR = System.err;

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
            ERR.println("usage: DexKitProbe <apk>");
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
        OUT.println("== classes using feed strings ==");
        ClassMatcher matcher = ClassMatcher.create().usingEqStrings(FEED_STRINGS);
        List<ClassData> classes = bridge.findClass(FindClass.create().matcher(matcher));
        classes.stream()
            .sorted(Comparator.comparingInt(DexKitProbe::scoreFeedClass).reversed()
                .thenComparing(ClassData::getName))
            .forEach(DexKitProbe::printClassSummary);
        OUT.println("count=" + classes.size());
    }

    private static void dumpAdapterSubclasses(DexKitBridge bridge, String superClass) {
        OUT.println("== adapter subclasses: " + superClass + " ==");
        ClassMatcher matcher = ClassMatcher.create().superClass(superClass);
        List<ClassData> classes = bridge.findClass(FindClass.create().matcher(matcher));
        classes.stream()
            .sorted(Comparator.comparingInt(DexKitProbe::scoreAdapterClass).reversed()
                .thenComparing(ClassData::getName))
            .limit(80)
            .forEach(cls -> {
                OUT.println(cls.getName());
                cls.getMethods().stream()
                    .filter(method -> method.getMethodName().equals("onBindViewHolder"))
                    .forEach(method -> OUT.println("  bind " + method.getMethodSign()));
            });
        OUT.println("count=" + classes.size());
    }

    private static void dumpDiscoverPackageListMethods(DexKitBridge bridge) {
        OUT.println("== discover package list-ish methods ==");
        List<ClassData> classes =
            bridge.findClass(FindClass.create().searchPackages("com.google.android.apps.search.googleapp.discover"));
        classes.stream()
            .sorted(Comparator.comparingInt(DexKitProbe::scoreDiscoverClass).reversed()
                .thenComparing(ClassData::getName))
            .forEach(cls -> cls.getMethods().stream()
                .sorted(Comparator.comparingInt(DexKitProbe::scoreListMethod).reversed()
                    .thenComparing(MethodData::getMethodName))
                .filter(method -> isListy(method.getReturnTypeName()) || looksLikeAccessor(method.getMethodName()))
                .forEach(method -> {
                    OUT.println(
                        cls.getName() + " :: " + method.getMethodSign() +
                            " strings=" + preview(method.getUsingStrings())
                    );
                }));
        OUT.println("discover-class-count=" + classes.size());
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

    private static int scoreFeedClass(ClassData cls) {
        String name = cls.getName().toLowerCase(Locale.ROOT);
        int score = 0;
        if (name.contains("discover")) score += 30;
        if (name.contains("stream")) score += 20;
        if (name.contains("card")) score += 20;
        if (name.contains("adapter")) score += 10;
        return score;
    }

    private static int scoreAdapterClass(ClassData cls) {
        String name = cls.getName().toLowerCase(Locale.ROOT);
        int score = 0;
        if (name.contains("discover")) score += 30;
        if (name.contains("stream")) score += 20;
        if (name.contains("card")) score += 15;
        if (name.contains("feed")) score += 10;
        return score;
    }

    private static int scoreDiscoverClass(ClassData cls) {
        String name = cls.getName().toLowerCase(Locale.ROOT);
        int score = 0;
        if (name.contains("stream")) score += 25;
        if (name.contains("render")) score += 15;
        if (name.contains("content")) score += 15;
        return score;
    }

    private static int scoreListMethod(MethodData method) {
        String name = method.getMethodName().toLowerCase(Locale.ROOT);
        int score = 0;
        if (looksLikeAccessor(method.getMethodName())) score += 30;
        if (name.contains("content")) score += 20;
        if (name.contains("element")) score += 15;
        if (name.contains("render")) score += 15;
        if (isListy(method.getReturnTypeName())) score += 10;
        return score;
    }

    private static void printClassSummary(ClassData cls) {
        OUT.println(cls.getName());
        cls.getMethods().stream()
            .filter(method -> isListy(method.getReturnTypeName()) || looksLikeAccessor(method.getMethodName()))
            .forEach(method -> OUT.println(
                "  " + method.getMethodSign() + " strings=" + preview(method.getUsingStrings())
            ));
    }

    private static String preview(List<String> strings) {
        if (strings == null || strings.isEmpty()) return "[]";
        return strings.stream().limit(8).toList().toString();
    }
}
