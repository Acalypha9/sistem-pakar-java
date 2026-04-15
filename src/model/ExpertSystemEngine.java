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

    public record IntermediateWeight(String nodeId, String label, double weight, int matched, int total, List<String> dependencies) {}

    public record WeightedDiseaseScore(String diseaseId, String diseaseName, String category, double percentage, List<IntermediateWeight> intermediateWeights) {}

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

    public static Map<String, IntermediateWeight> calculateIntermediateWeights(String type, Set<String> selectedQuestionIds) {
        KnowledgeBase kb = getKnowledgeBase(type);
        LinkedHashMap<String, IntermediateWeight> weights = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : kb.rules().entrySet()) {
            String nodeId = entry.getKey();
            if (kb.diseases().containsKey(nodeId)) continue;
            if (kb.questions().containsKey(nodeId)) continue;

            List<String> deps = entry.getValue();
            int matched = 0;
            for (String dep : deps) {
                if (selectedQuestionIds.contains(dep)) {
                    matched++;
                }
            }

            double weight = deps.isEmpty() ? 0.0 : (double) matched / deps.size() * 100.0;
            String label = getNodeLabel(type, nodeId);
            weights.put(nodeId, new IntermediateWeight(nodeId, label, weight, matched, deps.size(), deps));
        }

        return weights;
    }

    public static List<WeightedDiseaseScore> calculateWeightedDiseaseScores(String type, Set<String> selectedQuestionIds) {
        KnowledgeBase kb = getKnowledgeBase(type);
        Map<String, IntermediateWeight> intermediateWeights = calculateIntermediateWeights(type, selectedQuestionIds);

        List<WeightedDiseaseScore> result = new ArrayList<>();

        for (Disease disease : kb.diseases().values()) {
            List<String> deps = kb.rules().get(disease.id());
            if (deps == null || deps.isEmpty()) continue;

            List<IntermediateWeight> usedWeights = new ArrayList<>();
            double sumWeights = 0.0;

            for (String dep : deps) {
                IntermediateWeight iw = intermediateWeights.get(dep);
                if (iw != null) {
                    usedWeights.add(iw);
                    sumWeights += iw.weight();
                }
            }

            double divisor = deps.size() > 0 ? deps.size() : 1;
            double finalPercentage = sumWeights / divisor;
            result.add(new WeightedDiseaseScore(disease.id(), disease.name(), disease.category(), finalPercentage, usedWeights));
        }

        return result;
    }

    public static List<DiagnosisResult> diagnoseWeightedBased(String type, Set<String> selectedQuestionIds, double threshold) {
        List<WeightedDiseaseScore> scores = calculateWeightedDiseaseScores(type, selectedQuestionIds);
        List<DiagnosisResult> results = new ArrayList<>();

        for (WeightedDiseaseScore score : scores) {
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

        // === Sub-gejala Influenza (Pernafasan) ===
        builder.addQuestion("1", "Apakah suhu tubuh anda 37.5–39°C?", "Suhu 37.5–39°C");
        builder.addQuestion("2", "Apakah suhu tubuh meningkat bertahap?", "Suhu meningkat bertahap");
        builder.addQuestion("3", "Apakah anda mengalami menggigil ringan?", "Menggigil ringan");
        builder.addQuestion("4", "Apakah anda mengalami batuk kering?", "Batuk kering");
        builder.addQuestion("5", "Apakah anda berdahak ringan?", "Berdahak ringan");
        builder.addQuestion("6", "Apakah frekuensi batuk anda >3x/jam?", "Frekuensi batuk >3x/jam");
        builder.addQuestion("7", "Apakah hidung anda tersumbat?", "Hidung tersumbat");
        builder.addQuestion("8", "Apakah hidung anda berair?", "Hidung berair");
        builder.addQuestion("9", "Apakah cairan hidung bening dan encer?", "Cairan bening encer");
        builder.addQuestion("10", "Apakah anda bersin berulang?", "Bersin berulang");
        builder.addQuestion("11", "Apakah anda mengalami nyeri saat menelan?", "Nyeri saat menelan");
        builder.addQuestion("12", "Apakah terdapat kemerahan ringan pada faring?", "Kemerahan ringan pada faring");

        // === Sub-gejala Demam Berdarah (Infeksi Virus) ===
        builder.addQuestion("13", "Apakah suhu tubuh anda >39°C secara mendadak?", "Suhu >39°C mendadak");
        builder.addQuestion("14", "Apakah pola demam naik turun selama 2–7 hari?", "Pola 2–7 hari suhu turun naik");
        builder.addQuestion("15", "Apakah anda mengalami nyeri pada otot?", "Nyeri pada otot");
        builder.addQuestion("16", "Apakah anda mengalami nyeri pada tulang?", "Nyeri pada tulang");
        builder.addQuestion("17", "Apakah muncul bintik merah pada kulit?", "Bintik merah");
        builder.addQuestion("18", "Apakah bintik tidak hilang saat ditekan?", "Bintik tidak hilang saat ditekan");
        builder.addQuestion("19", "Apakah hasil uji torniket positif?", "Uji torniket positif");
        builder.addQuestion("20", "Apakah anda muntah >2x/hari?", "Muntah >2x/hari");
        builder.addQuestion("21", "Apakah nafsu makan anda turun?", "Nafsu makan turun");

        // === Sub-gejala Diabetes (Metabolik) ===
        builder.addQuestion("22", "Apakah anda minum >3 liter/hari?", "Minum >3 liter/hari");
        builder.addQuestion("23", "Apakah anda buang air kecil >8x/hari?", "Buang air kecil >8x/hari");
        builder.addQuestion("24", "Apakah anda sering buang air kecil pada malam hari?", "Sering buang air kecil malam hari");
        builder.addQuestion("25", "Apakah energi anda cepat habis?", "Energi cepat habis");
        builder.addQuestion("26", "Apakah kadar gula darah anda >200 mg/dL?", "Kadar gula darah >200 mg/dL");
        builder.addQuestion("27", "Apakah anda mengalami infeksi berulang?", "Infeksi berulang");
        builder.addQuestion("28", "Apakah penyembuhan luka anda >2 minggu?", "Penyembuhan luka >2 minggu");

        // === Sub-gejala Hipertensi (Kardiovaskular) ===
        builder.addQuestion("29", "Apakah sakit kepala terjadi pada pagi hari?", "Sakit kepala pagi hari");
        builder.addQuestion("30", "Apakah sakit kepala berdenyut?", "Sakit kepala berdenyut");
        builder.addQuestion("31", "Apakah nyeri terutama di bagian belakang kepala?", "Nyeri belakang kepala");
        builder.addQuestion("32", "Apakah anda merasa berputar?", "Terasa berputar");
        builder.addQuestion("33", "Apakah anda merasa ringan saat berubah posisi?", "Ringan saat berubah posisi");
        builder.addQuestion("34", "Apakah anda mengalami gangguan visual sementara?", "Gangguan visual sementara");
        builder.addQuestion("35", "Apakah retina mengalami penyempitan pembuluh?", "Penyempitan pembuluh retina");
        builder.addQuestion("36", "Apakah tekanan darah anda konsisten ≥140/90 mmHg?", "Tekanan darah ≥140/90 mmHg");
        builder.addQuestion("37", "Apakah detak jantung anda meningkat?", "Detak jantung meningkat");
        builder.addQuestion("38", "Apakah anda mengalami perdarahan dari hidung (mimisan)?", "Perdarahan dari hidung");

        // === Derived Nodes (Fakta Turunan) ===
        builder.addDerivedNode("39", "Demam");
        builder.addDerivedNode("40", "Batuk");
        builder.addDerivedNode("41", "Pilek");
        builder.addDerivedNode("42", "Sakit Tenggorokan");
        builder.addDerivedNode("43", "Demam Tinggi");
        builder.addDerivedNode("44", "Nyeri Sendi");
        builder.addDerivedNode("45", "Ruam Kulit");
        builder.addDerivedNode("46", "Mual");
        builder.addDerivedNode("47", "Sering Haus");
        builder.addDerivedNode("48", "Sering Buang Air Kecil");
        builder.addDerivedNode("49", "Mudah Lelah");
        builder.addDerivedNode("50", "Luka Sulit Sembuh");
        builder.addDerivedNode("51", "Sakit Kepala");
        builder.addDerivedNode("52", "Pusing");
        builder.addDerivedNode("53", "Penglihatan Kabur");
        builder.addDerivedNode("54", "Tekanan Darah Tinggi");
        builder.addDerivedNode("55", "Mimisan");

        // === Diseases ===
        builder.addDisease("56", "Influenza", "Infeksi",
                "Penyakit infeksi (menular) pada saluran pernafasan yang disebabkan oleh virus influenza. "
                        + "Gejala meliputi demam ringan (37.5-39°C) yang meningkat bertahap disertai menggigil ringan, "
                        + "batuk kering dengan dahak ringan (frekuensi >3x/jam), pilek dengan hidung tersumbat dan berair "
                        + "(cairan bening encer) disertai bersin berulang, serta sakit tenggorokan (nyeri saat menelan, "
                        + "kemerahan pada faring).",
                "Istirahat cukup, perbanyak minum air hangat, konsumsi makanan bergizi, hindari kontak dengan "
                        + "orang lain untuk mencegah penularan, gunakan masker, dan segera periksakan diri ke dokter "
                        + "bila gejala memburuk.");

        builder.addDisease("57", "Demam Berdarah", "Infeksi",
                "Penyakit infeksi (menular) yang disebabkan oleh virus dengue melalui gigitan nyamuk Aedes aegypti. "
                        + "Gejala meliputi demam tinggi mendadak (>39°C) dengan pola naik turun selama 2-7 hari, "
                        + "nyeri pada otot dan tulang, ruam kulit berupa bintik merah (tidak hilang saat ditekan, "
                        + "uji torniket positif), serta mual dengan muntah >2x/hari dan menurunnya nafsu makan.",
                "Perbanyak minum cairan, segera ke rumah sakit untuk pemantauan trombosit, hindari penggunaan "
                        + "aspirin, lakukan fogging dan 3M (Menguras, Menutup, Mengubur) untuk pencegahan nyamuk.");

        builder.addDisease("58", "Diabetes", "Non-Infeksi",
                "Penyakit non-infeksi (tidak menular/kronis) yang ditandai dengan tingginya kadar gula darah "
                        + "(>200 mg/dL). Gejala meliputi sering haus (minum >3 liter/hari), sering buang air kecil "
                        + "(>8x/hari terutama malam hari), mudah lelah dengan energi cepat habis, serta luka sulit "
                        + "sembuh (penyembuhan >2 minggu) dengan infeksi yang bisa berulang.",
                "Jaga pola makan sehat dan rendah gula, olahraga teratur, kontrol kadar gula darah secara "
                        + "berkala, konsumsi obat sesuai resep dokter, dan jaga berat badan ideal.");

        builder.addDisease("59", "Hipertensi", "Non-Infeksi",
                "Penyakit non-infeksi (tidak menular/kronis) yang ditandai dengan tekanan darah tinggi konsisten "
                        + "(≥140/90 mmHg). Gejala meliputi sakit kepala berdenyut terutama di bagian belakang, pusing "
                        + "berputar dan terasa ringan saat berubah posisi, penglihatan kabur (gangguan visual sementara, "
                        + "penyempitan pembuluh retina), tekanan darah tinggi dengan detak jantung meningkat, dan "
                        + "mimisan (perdarahan dari hidung).",
                "Kurangi konsumsi garam, olahraga teratur, kelola stres, hindari rokok dan alkohol, kontrol "
                        + "tekanan darah secara berkala, dan konsumsi obat antihipertensi sesuai resep dokter.");

        // === Rules: Derived nodes ← sub-gejala ===
        builder.addRule("39", "1", "2", "3");           // Demam ← suhu 37.5-39, meningkat bertahap, menggigil
        builder.addRule("40", "4", "5", "6");           // Batuk ← kering, dahak ringan, >3x/jam
        builder.addRule("41", "7", "8", "9", "10");     // Pilek ← tersumbat, berair, bening encer, bersin
        builder.addRule("42", "11", "12");              // Sakit Tenggorokan ← nyeri menelan, kemerahan faring
        builder.addRule("43", "13", "14");              // Demam Tinggi ← >39°C mendadak, pola 2-7 hari
        builder.addRule("44", "15", "16");              // Nyeri Sendi ← nyeri otot, nyeri tulang
        builder.addRule("45", "17", "18", "19");        // Ruam Kulit ← bintik merah, tidak hilang, torniket+
        builder.addRule("46", "20", "21");              // Mual ← muntah >2x, nafsu makan turun
        builder.addRule("47", "22");                    // Sering Haus ← minum >3L/hari
        builder.addRule("48", "23", "24");              // Sering BAK ← >8x/hari, malam hari
        builder.addRule("49", "25", "26");              // Mudah Lelah ← energi habis, gula >200
        builder.addRule("50", "27", "28");              // Luka Sulit Sembuh ← infeksi berulang, >2 minggu
        builder.addRule("51", "29", "30", "31");        // Sakit Kepala ← pagi, berdenyut, belakang
        builder.addRule("52", "32", "33");              // Pusing ← berputar, ringan saat posisi berubah
        builder.addRule("53", "34", "35");              // Penglihatan Kabur ← visual sementara, penyempitan
        builder.addRule("54", "36", "37");              // Tekanan Darah Tinggi ← >=140/90, detak meningkat
        builder.addRule("55", "38");                    // Mimisan ← perdarahan hidung

        // === Rules: Diseases ← derived nodes ===
        builder.addRule("56", "39", "40", "41", "42");          // Influenza ← Demam, Batuk, Pilek, Sakit Tenggorokan
        builder.addRule("57", "43", "44", "45", "46");          // Demam Berdarah ← Demam Tinggi, Nyeri Sendi, Ruam Kulit, Mual
        builder.addRule("58", "47", "48", "49", "50");          // Diabetes ← Sering Haus, Sering BAK, Mudah Lelah, Luka Sulit Sembuh
        builder.addRule("59", "51", "52", "53", "54", "55");    // Hipertensi ← Sakit Kepala, Pusing, Penglihatan Kabur, TD Tinggi, Mimisan

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
