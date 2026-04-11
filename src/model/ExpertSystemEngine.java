package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExpertSystemEngine {
    public static final double PERCENTAGE_THRESHOLD = 50.0;

    public record Question(String id, String text) {}

    public record Disease(String id, String name, String category, String description, String prevention) {}

    public record DiagnosisResult(String diseaseId, String diseaseName, String category, double percentage, boolean detected) {}

    public record DiseaseScore(String diseaseId, String diseaseName, String category, double percentage, int matched, int total) {}

    private static final LinkedHashMap<String, Question> QUESTIONS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Disease> DISEASES = new LinkedHashMap<>();
    private static final LinkedHashMap<String, List<String>> RULES = new LinkedHashMap<>();

    static {
        addQuestion("1", "Apakah anda mengalami demam ringan (suhu 37.5-39\u00B0C, meningkat bertahap, menggigil ringan)?");
        addQuestion("2", "Apakah anda mengalami batuk kering (berdahak ringan, frekuensi >3x/jam)?");
        addQuestion("3", "Apakah anda mengalami pilek (hidung tersumbat, berair, cairan bening encer, bersin berulang)?");
        addQuestion("4", "Apakah anda mengalami sakit tenggorokan (nyeri saat menelan, kemerahan pada faring)?");

        addQuestion("5", "Apakah anda mengalami demam tinggi mendadak (suhu >39\u00B0C, naik turun selama 2-7 hari)?");
        addQuestion("6", "Apakah anda mengalami nyeri sendi (nyeri pada otot dan tulang)?");
        addQuestion("7", "Apakah anda mengalami ruam kulit (bintik merah, tidak hilang saat ditekan, uji torniket positif)?");
        addQuestion("8", "Apakah anda mengalami mual (muntah >2x/hari, nafsu makan turun)?");

        addQuestion("9", "Apakah anda sering haus (minum >3 liter/hari)?");
        addQuestion("10", "Apakah anda sering buang air kecil (>8x/hari, terutama pada malam hari)?");
        addQuestion("11", "Apakah anda mudah lelah (energi cepat habis, kadar gula darah >200 mg/dL)?");
        addQuestion("12", "Apakah luka anda sulit sembuh (infeksi berulang, penyembuhan >2 minggu)?");

        addQuestion("13", "Apakah anda sering sakit kepala (berdenyut, terutama di bagian belakang kepala)?");
        addQuestion("14", "Apakah anda sering pusing (terasa berputar dan ringan saat berubah posisi)?");
        addQuestion("15", "Apakah penglihatan anda sering kabur (gangguan visual sementara, penyempitan pembuluh retina)?");
        addQuestion("16", "Apakah tekanan darah anda tinggi (konsisten \u2265140/90 mmHg, detak jantung meningkat)?");
        addQuestion("17", "Apakah anda mengalami mimisan (perdarahan dari hidung)?");

        addDisease("22", "Influenza", "Infeksi",
                "Penyakit infeksi (menular) pada saluran pernafasan yang disebabkan oleh virus influenza. "
                + "Gejala meliputi demam ringan (37.5-39\u00B0C) yang meningkat bertahap disertai menggigil ringan, "
                + "batuk kering dengan dahak ringan (frekuensi >3x/jam), pilek dengan hidung tersumbat dan berair "
                + "(cairan bening encer) disertai bersin berulang, serta sakit tenggorokan (nyeri saat menelan, "
                + "kemerahan pada faring).",
                "Istirahat cukup, perbanyak minum air hangat, konsumsi makanan bergizi, hindari kontak dengan "
                + "orang lain untuk mencegah penularan, gunakan masker, dan segera periksakan diri ke dokter "
                + "bila gejala memburuk.");

        addDisease("23", "Demam Berdarah", "Infeksi",
                "Penyakit infeksi (menular) yang disebabkan oleh virus dengue melalui gigitan nyamuk Aedes aegypti. "
                + "Gejala meliputi demam tinggi mendadak (>39\u00B0C) dengan pola naik turun selama 2-7 hari, "
                + "nyeri pada otot dan tulang, ruam kulit berupa bintik merah (tidak hilang saat ditekan, "
                + "uji torniket positif), serta mual dengan muntah >2x/hari dan menurunnya nafsu makan.",
                "Perbanyak minum cairan, segera ke rumah sakit untuk pemantauan trombosit, hindari penggunaan "
                + "aspirin, lakukan fogging dan 3M (Menguras, Menutup, Mengubur) untuk pencegahan nyamuk.");

        addDisease("24", "Diabetes", "Non-Infeksi",
                "Penyakit non-infeksi (tidak menular/kronis) yang ditandai dengan tingginya kadar gula darah "
                + "(>200 mg/dL). Gejala meliputi sering haus (minum >3 liter/hari), sering buang air kecil "
                + "(>8x/hari terutama malam hari), mudah lelah dengan energi cepat habis, serta luka sulit "
                + "sembuh (penyembuhan >2 minggu) dengan infeksi yang bisa berulang.",
                "Jaga pola makan sehat dan rendah gula, olahraga teratur, kontrol kadar gula darah secara "
                + "berkala, konsumsi obat sesuai resep dokter, dan jaga berat badan ideal.");

        addDisease("25", "Hipertensi", "Non-Infeksi",
                "Penyakit non-infeksi (tidak menular/kronis) yang ditandai dengan tekanan darah tinggi konsisten "
                + "(\u2265140/90 mmHg). Gejala meliputi sakit kepala berdenyut terutama di bagian belakang, pusing "
                + "berputar dan terasa ringan saat berubah posisi, penglihatan kabur (gangguan visual sementara, "
                + "penyempitan pembuluh retina), tekanan darah tinggi dengan detak jantung meningkat, dan "
                + "mimisan (perdarahan dari hidung).",
                "Kurangi konsumsi garam, olahraga teratur, kelola stres, hindari rokok dan alkohol, kontrol "
                + "tekanan darah secara berkala, dan konsumsi obat antihipertensi sesuai resep dokter.");

        addRule("22", "1", "2", "3", "4");
        addRule("23", "5", "6", "7", "8");
        addRule("24", "9", "10", "11", "12");
        addRule("25", "13", "14", "15", "16", "17");
    }

    private ExpertSystemEngine() {}

    private static void addQuestion(String id, String text) {
        QUESTIONS.put(id, new Question(id, text));
    }

    private static void addDisease(String id, String name, String category, String description, String prevention) {
        DISEASES.put(id, new Disease(id, name, category, description, prevention));
    }

    private static void addRule(String diseaseId, String... symptomIds) {
        RULES.put(diseaseId, Arrays.asList(symptomIds));
    }

    public static List<Question> getQuestions() {
        return new ArrayList<>(QUESTIONS.values());
    }

    public static List<Disease> getDiseases() {
        return new ArrayList<>(DISEASES.values());
    }

    public static Disease getDisease(String id) {
        return DISEASES.get(id);
    }

    public static List<String> getDependencies(String diseaseId) {
        return RULES.getOrDefault(diseaseId, Collections.emptyList());
    }

    public static Set<String> expandQuestionDependenciesForDisease(String diseaseId) {
        List<String> deps = RULES.get(diseaseId);
        if (deps == null) return Collections.emptySet();
        return new java.util.LinkedHashSet<>(deps);
    }

    public static List<DiagnosisResult> diagnoseRuleBased(Set<String> selectedQuestionIds) {
        List<DiagnosisResult> results = new ArrayList<>();
        for (Disease disease : DISEASES.values()) {
            List<String> required = RULES.get(disease.id());
            if (required != null && selectedQuestionIds.containsAll(required)) {
                results.add(new DiagnosisResult(disease.id(), disease.name(), disease.category(), 100.0, true));
            }
        }
        return results;
    }

    public static List<DiseaseScore> calculateDiseaseScores(Set<String> selectedQuestionIds) {
        List<DiseaseScore> result = new ArrayList<>();
        for (Disease disease : DISEASES.values()) {
            List<String> required = RULES.get(disease.id());
            if (required == null || required.isEmpty()) continue;
            int matched = 0;
            for (String symptomId : required) {
                if (selectedQuestionIds.contains(symptomId)) matched++;
            }
            double percentage = (double) matched / required.size() * 100.0;
            result.add(new DiseaseScore(disease.id(), disease.name(), disease.category(), percentage, matched, required.size()));
        }
        return result;
    }

    public static List<DiagnosisResult> diagnosePercentageBased(Set<String> selectedQuestionIds, double threshold) {
        List<DiseaseScore> scores = calculateDiseaseScores(selectedQuestionIds);
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
        if (ruleBased) return diagnoseRuleBased(selectedQuestionIds);
        return diagnosePercentageBased(selectedQuestionIds, PERCENTAGE_THRESHOLD);
    }
}
