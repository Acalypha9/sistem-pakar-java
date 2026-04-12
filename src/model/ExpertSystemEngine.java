package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExpertSystemEngine {
    public static final double PERCENTAGE_THRESHOLD = 50.0;
    public static final String TYPE_INFEKSI = "infeksi";
    public static final String TYPE_GASTROUSUS = "gastrousus";

    public record Question(String id, String text) {}

    public record Disease(String id, String name, String category, String description, String prevention) {}

    public record DiagnosisResult(String diseaseId, String diseaseName, String category, double percentage, boolean detected) {}

    public record DiseaseScore(String diseaseId, String diseaseName, String category, double percentage, int matched, int total) {}

    private record KnowledgeBase(
            String type,
            String displayName,
            LinkedHashMap<String, Question> questions,
            LinkedHashMap<String, String> labels,
            LinkedHashMap<String, Disease> diseases,
            LinkedHashMap<String, List<String>> rules
    ) {}

    private static final LinkedHashMap<String, KnowledgeBase> KNOWLEDGE_BASES = new LinkedHashMap<>();

    static {
        registerKnowledgeBase(buildInfeksiKnowledgeBase());
        registerKnowledgeBase(buildGastroususKnowledgeBase());
    }

    private ExpertSystemEngine() {}

    private static void registerKnowledgeBase(KnowledgeBase knowledgeBase) {
        KNOWLEDGE_BASES.put(knowledgeBase.type(), knowledgeBase);
    }

    public static String normalizeType(String type) {
        if (type == null) {
            return TYPE_INFEKSI;
        }

        String normalized = type.trim().toLowerCase();
        if (TYPE_GASTROUSUS.equals(normalized)) {
            return TYPE_GASTROUSUS;
        }
        return TYPE_INFEKSI;
    }

    public static String getDisplayName(String type) {
        return getKnowledgeBase(type).displayName();
    }

    public static List<String> getSupportedTypes() {
        return new ArrayList<>(KNOWLEDGE_BASES.keySet());
    }

    private static KnowledgeBase getKnowledgeBase(String type) {
        return KNOWLEDGE_BASES.get(normalizeType(type));
    }

    public static List<Question> getQuestions() {
        return getQuestions(TYPE_INFEKSI);
    }

    public static List<Question> getQuestions(String type) {
        return new ArrayList<>(getKnowledgeBase(type).questions().values());
    }

    public static List<Disease> getDiseases() {
        return getDiseases(TYPE_INFEKSI);
    }

    public static List<Disease> getDiseases(String type) {
        return new ArrayList<>(getKnowledgeBase(type).diseases().values());
    }

    public static Disease getDisease(String id) {
        for (KnowledgeBase knowledgeBase : KNOWLEDGE_BASES.values()) {
            Disease disease = knowledgeBase.diseases().get(id);
            if (disease != null) {
                return disease;
            }
        }
        return null;
    }

    public static Disease getDisease(String type, String id) {
        return getKnowledgeBase(type).diseases().get(id);
    }

    public static String getNodeLabel(String type, String nodeId) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(type);
        String label = knowledgeBase.labels().get(nodeId);
        if (label != null) {
            return label;
        }

        Disease disease = knowledgeBase.diseases().get(nodeId);
        return disease != null ? disease.name() : nodeId;
    }

    public static String getQuestionText(String type, String questionId) {
        Question question = getKnowledgeBase(type).questions().get(questionId);
        if (question != null) {
            return question.text();
        }
        return getNodeLabel(type, questionId);
    }

    public static List<String> getDependencies(String diseaseId) {
        return getDependencies(TYPE_INFEKSI, diseaseId);
    }

    public static List<String> getDependencies(String type, String diseaseId) {
        return getKnowledgeBase(type).rules().getOrDefault(diseaseId, Collections.emptyList());
    }

    public static Set<String> expandQuestionDependenciesForDisease(String diseaseId) {
        return expandQuestionDependenciesForDisease(TYPE_INFEKSI, diseaseId);
    }

    public static Set<String> expandQuestionDependenciesForDisease(String type, String diseaseId) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(type);
        if (!knowledgeBase.rules().containsKey(diseaseId)) {
            return Collections.emptySet();
        }

        LinkedHashSet<String> leafQuestions = new LinkedHashSet<>();
        expandLeafQuestions(knowledgeBase, diseaseId, leafQuestions, new LinkedHashSet<>());
        return leafQuestions;
    }

    private static void expandLeafQuestions(
            KnowledgeBase knowledgeBase,
            String nodeId,
            Set<String> leafQuestions,
            Set<String> visiting
    ) {
        if (!visiting.add(nodeId)) {
            return;
        }

        List<String> dependencies = knowledgeBase.rules().get(nodeId);
        if (dependencies == null || dependencies.isEmpty()) {
            if (knowledgeBase.questions().containsKey(nodeId)) {
                leafQuestions.add(nodeId);
            }
            visiting.remove(nodeId);
            return;
        }

        for (String dependency : dependencies) {
            expandLeafQuestions(knowledgeBase, dependency, leafQuestions, visiting);
        }

        visiting.remove(nodeId);
    }

    public static Set<String> inferFacts(Set<String> selectedQuestionIds) {
        return inferFacts(TYPE_INFEKSI, selectedQuestionIds);
    }

    public static Set<String> inferFacts(String type, Set<String> selectedQuestionIds) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(type);
        LinkedHashSet<String> facts = new LinkedHashSet<>(selectedQuestionIds);

        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<String>> entry : knowledgeBase.rules().entrySet()) {
                if (facts.contains(entry.getKey())) {
                    continue;
                }
                if (facts.containsAll(entry.getValue())) {
                    facts.add(entry.getKey());
                    changed = true;
                }
            }
        } while (changed);

        return facts;
    }

    public static List<DiagnosisResult> diagnoseRuleBased(Set<String> selectedQuestionIds) {
        return diagnoseRuleBased(TYPE_INFEKSI, selectedQuestionIds);
    }

    public static List<DiagnosisResult> diagnoseRuleBased(String type, Set<String> selectedQuestionIds) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(type);
        Set<String> inferredFacts = inferFacts(type, selectedQuestionIds);
        List<DiagnosisResult> results = new ArrayList<>();

        for (Disease disease : knowledgeBase.diseases().values()) {
            if (inferredFacts.contains(disease.id())) {
                results.add(new DiagnosisResult(disease.id(), disease.name(), disease.category(), 100.0, true));
            }
        }

        return results;
    }

    public static List<DiseaseScore> calculateDiseaseScores(Set<String> selectedQuestionIds) {
        return calculateDiseaseScores(TYPE_INFEKSI, selectedQuestionIds);
    }

    public static List<DiseaseScore> calculateDiseaseScores(String type, Set<String> selectedQuestionIds) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(type);
        List<DiseaseScore> result = new ArrayList<>();

        for (Disease disease : knowledgeBase.diseases().values()) {
            Set<String> required = expandQuestionDependenciesForDisease(type, disease.id());
            if (required == null || required.isEmpty()) {
                continue;
            }

            int matched = 0;
            for (String symptomId : required) {
                if (selectedQuestionIds.contains(symptomId)) {
                    matched++;
                }
            }

            double percentage = (double) matched / required.size() * 100.0;
            result.add(new DiseaseScore(disease.id(), disease.name(), disease.category(), percentage, matched, required.size()));
        }

        return result;
    }

    public static List<DiagnosisResult> diagnosePercentageBased(Set<String> selectedQuestionIds, double threshold) {
        return diagnosePercentageBased(TYPE_INFEKSI, selectedQuestionIds, threshold);
    }

    public static List<DiagnosisResult> diagnosePercentageBased(String type, Set<String> selectedQuestionIds, double threshold) {
        List<DiseaseScore> scores = calculateDiseaseScores(type, selectedQuestionIds);
        List<DiagnosisResult> results = new ArrayList<>();

        for (DiseaseScore score : scores) {
            if (score.percentage() >= threshold) {
                results.add(new DiagnosisResult(score.diseaseId(), score.diseaseName(), score.category(), score.percentage(), true));
            }
        }

        results.sort((a, b) -> Double.compare(b.percentage(), a.percentage()));
        return results;
    }

    public static List<DiagnosisResult> diagnose(Set<String> selectedQuestionIds, boolean ruleBased) {
        return diagnose(TYPE_INFEKSI, selectedQuestionIds, ruleBased);
    }

    public static List<DiagnosisResult> diagnose(String type, Set<String> selectedQuestionIds, boolean ruleBased) {
        if (ruleBased) {
            return diagnoseRuleBased(type, selectedQuestionIds);
        }
        return diagnosePercentageBased(type, selectedQuestionIds, PERCENTAGE_THRESHOLD);
    }

    private static KnowledgeBase buildInfeksiKnowledgeBase() {
        KnowledgeBaseBuilder builder = new KnowledgeBaseBuilder(TYPE_INFEKSI, "Infeksi dan Non Infeksi");

        builder.addQuestion("1", "Apakah anda mengalami demam ringan (suhu 37.5-39°C, meningkat bertahap, menggigil ringan)?", "demam");
        builder.addQuestion("2", "Apakah anda mengalami batuk kering (berdahak ringan, frekuensi >3x/jam)?", "batuk");
        builder.addQuestion("3", "Apakah anda mengalami pilek (hidung tersumbat, berair, cairan bening encer, bersin berulang)?", "pilek");
        builder.addQuestion("4", "Apakah anda mengalami sakit tenggorokan (nyeri saat menelan, kemerahan pada faring)?", "sakit tenggorokan");
        builder.addQuestion("5", "Apakah anda mengalami demam tinggi mendadak (suhu >39°C, naik turun selama 2-7 hari)?", "demam tinggi");
        builder.addQuestion("6", "Apakah anda mengalami nyeri sendi (nyeri pada otot dan tulang)?", "nyeri sendi");
        builder.addQuestion("7", "Apakah anda mengalami ruam kulit (bintik merah, tidak hilang saat ditekan, uji torniket positif)?", "ruam kulit");
        builder.addQuestion("8", "Apakah anda mengalami mual (muntah >2x/hari, nafsu makan turun)?", "mual");
        builder.addQuestion("9", "Apakah anda sering haus (minum >3 liter/hari)?", "sering haus");
        builder.addQuestion("10", "Apakah anda sering buang air kecil (>8x/hari, terutama pada malam hari)?", "sering buang air kecil");
        builder.addQuestion("11", "Apakah anda mudah lelah (energi cepat habis, kadar gula darah >200 mg/dL)?", "mudah lelah");
        builder.addQuestion("12", "Apakah luka anda sulit sembuh (infeksi berulang, penyembuhan >2 minggu)?", "luka sulit sembuh");
        builder.addQuestion("13", "Apakah anda sering sakit kepala (berdenyut, terutama di bagian belakang kepala)?", "sakit kepala");
        builder.addQuestion("14", "Apakah anda sering pusing (terasa berputar dan ringan saat berubah posisi)?", "pusing");
        builder.addQuestion("15", "Apakah penglihatan anda sering kabur (gangguan visual sementara, penyempitan pembuluh retina)?", "penglihatan kabur");
        builder.addQuestion("16", "Apakah tekanan darah anda tinggi (konsisten ≥140/90 mmHg, detak jantung meningkat)?", "tekanan darah tinggi");
        builder.addQuestion("17", "Apakah anda mengalami mimisan (perdarahan dari hidung)?", "mimisan");

        builder.addDisease("22", "Influenza", "Infeksi",
                "Penyakit infeksi (menular) pada saluran pernafasan yang disebabkan oleh virus influenza. "
                        + "Gejala meliputi demam ringan (37.5-39°C) yang meningkat bertahap disertai menggigil ringan, "
                        + "batuk kering dengan dahak ringan (frekuensi >3x/jam), pilek dengan hidung tersumbat dan berair "
                        + "(cairan bening encer) disertai bersin berulang, serta sakit tenggorokan (nyeri saat menelan, "
                        + "kemerahan pada faring).",
                "Istirahat cukup, perbanyak minum air hangat, konsumsi makanan bergizi, hindari kontak dengan "
                        + "orang lain untuk mencegah penularan, gunakan masker, dan segera periksakan diri ke dokter "
                        + "bila gejala memburuk.");

        builder.addDisease("23", "Demam Berdarah", "Infeksi",
                "Penyakit infeksi (menular) yang disebabkan oleh virus dengue melalui gigitan nyamuk Aedes aegypti. "
                        + "Gejala meliputi demam tinggi mendadak (>39°C) dengan pola naik turun selama 2-7 hari, "
                        + "nyeri pada otot dan tulang, ruam kulit berupa bintik merah (tidak hilang saat ditekan, "
                        + "uji torniket positif), serta mual dengan muntah >2x/hari dan menurunnya nafsu makan.",
                "Perbanyak minum cairan, segera ke rumah sakit untuk pemantauan trombosit, hindari penggunaan "
                        + "aspirin, lakukan fogging dan 3M (Menguras, Menutup, Mengubur) untuk pencegahan nyamuk.");

        builder.addDisease("24", "Diabetes", "Non-Infeksi",
                "Penyakit non-infeksi (tidak menular/kronis) yang ditandai dengan tingginya kadar gula darah "
                        + "(>200 mg/dL). Gejala meliputi sering haus (minum >3 liter/hari), sering buang air kecil "
                        + "(>8x/hari terutama malam hari), mudah lelah dengan energi cepat habis, serta luka sulit "
                        + "sembuh (penyembuhan >2 minggu) dengan infeksi yang bisa berulang.",
                "Jaga pola makan sehat dan rendah gula, olahraga teratur, kontrol kadar gula darah secara "
                        + "berkala, konsumsi obat sesuai resep dokter, dan jaga berat badan ideal.");

        builder.addDisease("25", "Hipertensi", "Non-Infeksi",
                "Penyakit non-infeksi (tidak menular/kronis) yang ditandai dengan tekanan darah tinggi konsisten "
                        + "(≥140/90 mmHg). Gejala meliputi sakit kepala berdenyut terutama di bagian belakang, pusing "
                        + "berputar dan terasa ringan saat berubah posisi, penglihatan kabur (gangguan visual sementara, "
                        + "penyempitan pembuluh retina), tekanan darah tinggi dengan detak jantung meningkat, dan "
                        + "mimisan (perdarahan dari hidung).",
                "Kurangi konsumsi garam, olahraga teratur, kelola stres, hindari rokok dan alkohol, kontrol "
                        + "tekanan darah secara berkala, dan konsumsi obat antihipertensi sesuai resep dokter.");

        builder.addRule("22", "1", "2", "3", "4");
        builder.addRule("23", "5", "6", "7", "8");
        builder.addRule("24", "9", "10", "11", "12");
        builder.addRule("25", "13", "14", "15", "16", "17");

        return builder.build();
    }

    private static KnowledgeBase buildGastroususKnowledgeBase() {
        KnowledgeBaseBuilder builder = new KnowledgeBaseBuilder(TYPE_GASTROUSUS, "Gastrousus");

        builder.addQuestion("1", "Buang air besar (lebih dari 2 kali)", "Buang air besar (lebih dari 2 kali)");
        builder.addQuestion("2", "Berak encer", "Berak encer");
        builder.addQuestion("3", "Berak berdarah", "Berak berdarah");
        builder.addQuestion("4", "Lesu dan tidak bergairah", "Lesu dan tidak bergairah");
        builder.addQuestion("5", "Tidak selera makan", "Tidak selera makan");
        builder.addQuestion("6", "Merasa mual dan sering muntah (lebih dari 1 kali)", "Merasa mual dan sering muntah (lebih dari 1 kali)");
        builder.addQuestion("7", "Merasa sakit di bagian perut", "Merasa sakit di bagian perut");
        builder.addQuestion("8", "Tekanan darah rendah", "Tekanan darah rendah");
        builder.addQuestion("9", "Pusing", "Pusing");
        builder.addQuestion("10", "Pingsan", "Pingsan");
        builder.addQuestion("11", "Suhu badan tinggi", "Suhu badan tinggi");
        builder.addQuestion("12", "Luka di bagian tertentu", "Luka di bagian tertentu");
        builder.addQuestion("13", "Tidak dapat menggerakkan anggota badan tertentu", "Tidak dapat menggerakkan anggota badan tertentu");
        builder.addQuestion("14", "Memakan sesuatu", "Memakan sesuatu");
        builder.addQuestion("15", "Memakan daging", "Memakan daging");
        builder.addQuestion("16", "Memakan jamur", "Memakan jamur");
        builder.addQuestion("17", "Memakan makanan kaleng", "Memakan makanan kaleng");
        builder.addQuestion("18", "Membeli susu", "Membeli susu");
        builder.addQuestion("19", "Meminum susu", "Meminum susu");

        builder.addDerivedNode("20", "Mencret");
        builder.addDerivedNode("21", "Muntah");
        builder.addDerivedNode("22", "Sakit perut");
        builder.addDerivedNode("23", "Darah rendah");
        builder.addDerivedNode("24", "Koma");
        builder.addDerivedNode("25", "Demam");
        builder.addDerivedNode("26", "Septicaemia");
        builder.addDerivedNode("27", "Lumpuh");
        builder.addDerivedNode("28", "Mencret berdarah");
        builder.addDerivedNode("29", "Makan daging");
        builder.addDerivedNode("30", "Makan jamur");
        builder.addDerivedNode("31", "Makan makanan kaleng");
        builder.addDerivedNode("32", "Minum susu");

        builder.addDisease("33", "Keracunan Staphylococcus aureus", "Gastrousus",
                "Kemungkinan keracunan makanan akibat bakteri Staphylococcus aureus berdasarkan pola gejala gastrousus yang dipilih.",
                "Jaga kebersihan makanan, hindari makanan yang terkontaminasi, cukupi cairan tubuh, dan segera periksa ke tenaga medis jika gejala memburuk.");
        builder.addDisease("34", "Keracunan jamur beracun", "Gastrousus",
                "Kemungkinan keracunan akibat konsumsi jamur beracun berdasarkan pola gejala gastrousus yang dipilih.",
                "Hindari konsumsi jamur yang tidak jelas jenisnya, simpan sampel makanan bila memungkinkan, dan segera cari pertolongan medis.");
        builder.addDisease("35", "Keracunan Salmonellae", "Gastrousus",
                "Kemungkinan infeksi atau keracunan makanan akibat Salmonellae berdasarkan pola gejala gastrousus yang dipilih.",
                "Masak makanan hingga matang, jaga kebersihan bahan makanan, cukupkan cairan, dan lakukan pemeriksaan medis bila keluhan berat atau berlanjut.");
        builder.addDisease("36", "Keracunan Clostridium botulinum", "Gastrousus",
                "Kemungkinan keracunan botulinum dari makanan berdasarkan pola gejala gastrousus yang dipilih.",
                "Hindari makanan kaleng yang rusak, jangan konsumsi makanan yang berbau atau tampak tidak normal, dan segera ke fasilitas kesehatan karena kondisi ini dapat berbahaya.");
        builder.addDisease("37", "Keracunan Campylobacter", "Gastrousus",
                "Kemungkinan gangguan gastrousus akibat Campylobacter berdasarkan pola gejala yang dipilih.",
                "Pastikan makanan dan susu dikonsumsi dalam kondisi aman, jaga kebersihan pengolahan makanan, dan periksakan diri jika diare berdarah atau demam tinggi terjadi.");

        builder.addRule("20", "1", "2", "4", "5");
        builder.addRule("21", "4", "5", "6");
        builder.addRule("22", "4", "7");
        builder.addRule("23", "4", "8", "9");
        builder.addRule("24", "8", "10");
        builder.addRule("25", "4", "5", "9", "11");
        builder.addRule("26", "4", "8", "11", "12");
        builder.addRule("27", "4", "13");
        builder.addRule("28", "1", "2", "3", "4", "5");
        builder.addRule("29", "14", "15");
        builder.addRule("30", "14", "16");
        builder.addRule("31", "14", "17");
        builder.addRule("32", "18", "19");
        builder.addRule("33", "20", "21", "22", "23", "29");
        builder.addRule("34", "20", "21", "22", "24", "30");
        builder.addRule("35", "20", "21", "22", "25", "26", "29");
        builder.addRule("36", "21", "27", "31");
        builder.addRule("37", "28", "22", "25", "32");

        return builder.build();
    }

    private static final class KnowledgeBaseBuilder {
        private final String type;
        private final String displayName;
        private final LinkedHashMap<String, Question> questions = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        private final LinkedHashMap<String, Disease> diseases = new LinkedHashMap<>();
        private final LinkedHashMap<String, List<String>> rules = new LinkedHashMap<>();

        private KnowledgeBaseBuilder(String type, String displayName) {
            this.type = type;
            this.displayName = displayName;
        }

        private void addQuestion(String id, String text, String label) {
            questions.put(id, new Question(id, text));
            labels.put(id, label);
        }

        private void addDerivedNode(String id, String label) {
            labels.put(id, label);
        }

        private void addDisease(String id, String name, String category, String description, String prevention) {
            diseases.put(id, new Disease(id, name, category, description, prevention));
            labels.put(id, name);
        }

        private void addRule(String nodeId, String... dependencyIds) {
            rules.put(nodeId, Arrays.asList(dependencyIds));
        }

        private KnowledgeBase build() {
            return new KnowledgeBase(type, displayName, questions, labels, diseases, rules);
        }
    }
}
