package io.github.lithum12.obfuscate_lib_compatible;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Obfuscate_lib_compatible implements ITransformationService {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MIXIN_CONFIG = "obfuscate.mixins.json";
    private static final String MODS_TOML = "META-INF/mods.toml";

    @Nonnull @Override
    public String name() { return "obfuscate_lib_compatible"; }

    @Override
    public void initialize(IEnvironment environment) {
        File modsDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
                .map(Path::toFile)
                .map(d -> new File(d, "mods"))
                .orElseGet(() -> new File("mods"));
        applyFix(modsDir);
    }

    @Override public void beginScanning(IEnvironment environment) {}
    @Override public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {}
    @Nonnull @Override @SuppressWarnings("rawtypes")
    public List<ITransformer> transformers() { return Collections.emptyList(); }

    private void applyFix(File modsDir) {
        try {
            File jar = findTargetJar(modsDir);
            if (jar == null) { LOGGER.warn("[ObfuscateLibCompatible] Obfuscate not found in: {}", modsDir.getAbsolutePath()); return; }
            LOGGER.info("[ObfuscateLibCompatible] Found: {}", jar.getName());
            if (isAlreadyFixed(jar)) { LOGGER.info("[ObfuscateLibCompatible] Already fixed, skipping"); return; }
            modifyJar(jar);
            LOGGER.info("[ObfuscateLibCompatible] Fix applied");
        } catch (Exception e) {
            LOGGER.error("[ObfuscateLibCompatible] Fix failed", e);
        }
    }

    private File findTargetJar(File modsDir) {
        if (!modsDir.isDirectory()) return null;
        File[] jars = modsDir.listFiles((d, n) -> n.endsWith(".jar"));
        if (jars == null) return null;
        for (File jar : jars) if (isTargetMod(jar)) return jar;
        return null;
    }

    private boolean isTargetMod(File jar) {
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry(MODS_TOML);
            if (entry == null) return false;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), "UTF-8"))) {
                String content = r.lines().reduce("", (a, b) -> a + b);
                return content.contains("modId") && (content.contains("\"obfuscate\"") || content.contains("'obfuscate'"));
            }
        } catch (IOException e) { LOGGER.debug("Cannot read {}: {}", jar.getName(), e.getMessage()); }
        return false;
    }

    private boolean isAlreadyFixed(File jar) {
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry(MIXIN_CONFIG);
            if (entry == null) return true;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), "UTF-8"))) {
                String content = r.lines().reduce("", (a, b) -> a + b);
                return content.contains("\"required\":false") || content.contains("\"required\": false");
            }
        } catch (IOException e) { LOGGER.debug("Cannot check fix status: {}", e.getMessage()); }
        return false;
    }

    private void modifyJar(File jar) throws IOException {
        File backup = new File(jar.getParent(), jar.getName() + ".bak");
        if (!backup.exists()) {
            copyFile(jar, backup);
            LOGGER.info("[ObfuscateLibCompatible] Backup saved: {}", backup.getName());
        }

        File temp = File.createTempFile("obf_fix_", ".jar");
        try {
            try (ZipFile src = new ZipFile(jar);
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(temp))) {
                Enumeration<? extends ZipEntry> entries = src.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    if (entry.getName().equals(MIXIN_CONFIG)) {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(src.getInputStream(entry), "UTF-8"))) {
                            zos.write(modifyConfig(r.lines().reduce("", (a, b) -> a + b)).getBytes("UTF-8"));
                        }
                    } else {
                        copyStream(src.getInputStream(entry), zos);
                    }
                    zos.closeEntry();
                }
            }
            if (!temp.renameTo(jar)) copyFile(temp, jar);
        } finally {
            temp.delete();
        }
    }

    private String modifyConfig(String json) {
        try {
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
            if (obj.has("required")) {
                obj.addProperty("required", false);
                return obj.toString();
            }
        } catch (Exception e) { LOGGER.debug("JSON parse failed, using string replace"); }
        return json.replace("\"required\": true", "\"required\": false")
                .replace("\"required\":true", "\"required\":false");
    }

    private void copyFile(File src, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(src); FileOutputStream fos = new FileOutputStream(dest)) {
            copyStream(fis, fos);
        }
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
    }
}