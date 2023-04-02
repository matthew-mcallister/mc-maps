package mapgen;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.*;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class ServerMain {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    public static DedicatedServer startServer(String[] $$0) throws Exception {
        LOGGER.debug("Launching app");

        SharedConstants.tryDetectVersion();
        OptionParser optionParser = new OptionParser();
        OptionSpec noGui = optionParser.accepts("nogui");
        OptionSpec $$3 = optionParser.accepts("initSettings",
                "Initializes 'server.properties' and 'eula.txt', then quits");
        OptionSpec $$4 = optionParser.accepts("demo");
        OptionSpec $$5 = optionParser.accepts("bonusChest");
        OptionSpec $$6 = optionParser.accepts("forceUpgrade");
        OptionSpec eraseCache = optionParser.accepts("eraseCache");
        OptionSpec $$8 = optionParser.accepts("safeMode", "Loads level with vanilla datapack only");
        OptionSpec $$9 = optionParser.accepts("help").forHelp();
        OptionSpec $$10 = optionParser.accepts("singleplayer").withRequiredArg();
        OptionSpec $$11 = optionParser.accepts("universe").withRequiredArg().defaultsTo(".", new String[0]);
        OptionSpec $$12 = optionParser.accepts("world").withRequiredArg();
        OptionSpec port = optionParser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1,
                new Integer[0]);
        OptionSpec serverId = optionParser.accepts("serverId").withRequiredArg();
        OptionSpec $$15 = optionParser.accepts("jfrProfile");
        OptionSpec $$16 = optionParser.accepts("pidFile").withRequiredArg()
                .withValuesConvertedBy(new PathConverter(new PathProperties[0]));
        OptionSpec $$17 = optionParser.nonOptions();

        OptionSet optionSet = optionParser.parse($$0);
        if (optionSet.has($$9)) {
            optionParser.printHelpOn(System.err);
            return null;
        }

        Path $$19 = (Path) optionSet.valueOf($$16);
        if ($$19 != null) {
            writePidFile($$19);
        }

        CrashReport.preload();
        if (optionSet.has($$15)) {
            JvmProfiler.INSTANCE.start(Environment.SERVER);
        }

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.err.println("you dun goofed");
            }
        });
        Bootstrap.bootStrap();
        Bootstrap.validate();
        Util.startTimerHackThread();
        Path $$20 = Paths.get("server.properties");
        DedicatedServerSettings $$21 = new DedicatedServerSettings($$20);
        $$21.forceSave();
        Path $$22 = Paths.get("eula.txt");
        Eula $$23 = new Eula($$22);
        if (optionSet.has($$3)) {
            LOGGER.info("Initialized '{}' and '{}'", $$20.toAbsolutePath(), $$22.toAbsolutePath());
            return null;
        }

        if (!$$23.hasAgreedToEULA()) {
            LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
            return null;
        }

        File $$24 = new File((String) optionSet.valueOf($$11));
        Services $$25 = Services.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), $$24);
        String $$26 = (String) Optional.ofNullable((String) optionSet.valueOf($$12))
                .orElse($$21.getProperties().levelName);
        LevelStorageSource $$27 = LevelStorageSource.createDefault($$24.toPath());
        LevelStorageSource.LevelStorageAccess $$28 = $$27.createAccess($$26);
        LevelSummary $$29 = $$28.getSummary();
        if ($$29 != null) {
            if ($$29.requiresManualConversion()) {
                LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
                return null;
            }

            if (!$$29.isCompatible()) {
                LOGGER.info("This world was created by an incompatible version.");
                return null;
            }
        }

        boolean $$30 = optionSet.has($$8);
        if ($$30) {
            LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
        }

        PackRepository $$31 = ServerPacksSource.createPackRepository($$28.getLevelPath(LevelResource.DATAPACK_DIR));

        WorldStem worldStem;
        try {
            WorldLoader.InitConfig $$32 = loadOrCreateConfig($$21.getProperties(), $$28, $$30, $$31);
            worldStem = (WorldStem) Util.blockUntilDone(($$6x) -> WorldLoader.load($$32, ($$5x) -> {
                Registry $$96 = $$5x.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
                DynamicOps $$95 = RegistryOps.create(NbtOps.INSTANCE, $$5x.datapackWorldgen());
                Pair $$94 = $$28.getDataTag($$95, $$5x.dataConfiguration(), $$96,
                        $$5x.datapackWorldgen().allRegistriesLifecycle());
                if ($$94 != null) {
                    return new WorldLoader.DataLoadOutput((WorldData) $$94.getFirst(),
                            ((WorldDimensions.Complete) $$94.getSecond()).dimensionsRegistryAccess());
                } else {
                    LevelSettings levelSettings;
                    WorldOptions worldOptions;
                    WorldDimensions worldDimensions;
                    if (optionSet.has($$4)) {
                        levelSettings = MinecraftServer.DEMO_SETTINGS;
                        worldOptions = WorldOptions.DEMO_OPTIONS;
                        worldDimensions = WorldPresets.createNormalWorldDimensions($$5x.datapackWorldgen());
                    } else {
                        DedicatedServerProperties srvProps = $$21.getProperties();
                        levelSettings = new LevelSettings(srvProps.levelName, srvProps.gamemode, srvProps.hardcore,
                                srvProps.difficulty,
                                false, new GameRules(), $$5x.dataConfiguration());
                        worldOptions = optionSet.has($$5) ? srvProps.worldOptions.withBonusChest(true)
                                : srvProps.worldOptions;
                        worldDimensions = srvProps.createDimensions($$5x.datapackWorldgen());
                    }

                    WorldDimensions.Complete completeDimensions = worldDimensions.bake($$96);
                    Lifecycle $$93 = completeDimensions.lifecycle()
                            .add($$5x.datapackWorldgen().allRegistriesLifecycle());
                    return new WorldLoader.DataLoadOutput(
                            new PrimaryLevelData(levelSettings, worldOptions,
                                    completeDimensions.specialWorldProperty(), $$93),
                            completeDimensions.dimensionsRegistryAccess());
                }
            }, WorldStem::new, Util.backgroundExecutor(), $$6x)).get();
        } catch (Exception var37) {
            LOGGER.warn(
                    "Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode",
                    var37);
            return null;
        }

        RegistryAccess.Frozen $$36 = worldStem.registries().compositeAccess();
        if (optionSet.has($$6)) {
            forceUpgrade($$28, DataFixers.getDataFixer(), optionSet.has(eraseCache), () -> true,
                    $$36.registryOrThrow(Registries.LEVEL_STEM));
        }

        WorldData $$37 = worldStem.worldData();
        $$28.saveDataTag($$36, $$37);
        final DedicatedServer server = (DedicatedServer) MinecraftServer.spin(($$12x) -> {
            DedicatedServer dedicatedServer = new DedicatedServer($$12x, $$28, $$31, worldStem, $$21,
                    DataFixers.getDataFixer(),
                    $$25, LoggerChunkProgressListener::new);
            dedicatedServer.setSingleplayerProfile(
                    optionSet.has($$10) ? new GameProfile((UUID) null, (String) optionSet.valueOf($$10)) : null);
            dedicatedServer.setPort(((Integer) optionSet.valueOf(port)).intValue());
            dedicatedServer.setDemo(optionSet.has($$4));
            // dedicatedServer.setId((String) optionSet.valueOf(serverId));
            boolean b = !optionSet.has(noGui) && !optionSet.valuesOf($$17).contains("nogui");
            if (b && !GraphicsEnvironment.isHeadless()) {
                dedicatedServer.showGui();
            }

            return dedicatedServer;
        });
        Thread $$39 = new Thread("Server Shutdown Thread") {
            public void run() {
                server.halt(true);
            }
        };
        $$39.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        Runtime.getRuntime().addShutdownHook($$39);

        return server;
    }

    private static void writePidFile(Path $$0) {
        try {
            long $$1 = ProcessHandle.current().pid();
            Files.writeString($$0, Long.toString($$1));
        } catch (IOException var3) {
            throw new UncheckedIOException(var3);
        }
    }

    private static WorldLoader.InitConfig loadOrCreateConfig(DedicatedServerProperties $$0,
            LevelStorageSource.LevelStorageAccess $$1, boolean $$2, PackRepository $$3) {
        WorldDataConfiguration $$4 = $$1.getDataConfiguration();
        WorldDataConfiguration $$6;
        boolean $$5;
        if ($$4 != null) {
            $$5 = false;
            $$6 = $$4;
        } else {
            $$5 = true;
            $$6 = new WorldDataConfiguration($$0.initialDataPackConfiguration, FeatureFlags.DEFAULT_FLAGS);
        }

        WorldLoader.PackConfig $$9 = new WorldLoader.PackConfig($$3, $$6, $$2, $$5);
        return new WorldLoader.InitConfig($$9, Commands.CommandSelection.DEDICATED, $$0.functionPermissionLevel);
    }

    private static void forceUpgrade(LevelStorageSource.LevelStorageAccess $$0, DataFixer $$1, boolean $$2,
            BooleanSupplier $$3, Registry $$4) {
        LOGGER.info("Forcing world upgrade!");
        WorldUpgrader $$5 = new WorldUpgrader($$0, $$1, $$4, $$2);
        Component $$6 = null;

        while (!$$5.isFinished()) {
            Component $$7 = $$5.getStatus();
            if ($$6 != $$7) {
                $$6 = $$7;
                LOGGER.info($$5.getStatus().getString());
            }

            int $$8 = $$5.getTotalChunks();
            if ($$8 > 0) {
                int $$9 = $$5.getConverted() + $$5.getSkipped();
                LOGGER.info("{}% completed ({} / {} chunks)...",
                        new Object[] { Mth.floor((float) $$9 / (float) $$8 * 100.0F), $$9, $$8 });
            }

            if (!$$3.getAsBoolean()) {
                $$5.cancel();
            } else {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException var10) {
                }
            }
        }

    }
}
