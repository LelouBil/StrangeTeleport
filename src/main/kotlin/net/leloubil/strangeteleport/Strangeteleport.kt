package net.leloubil.strangeteleport

import net.leloubil.strangeteleport.block.Blocks
import net.leloubil.strangeteleport.blockentities.BlockEntities
import net.leloubil.strangeteleport.data.Attachments
import net.leloubil.strangeteleport.items.Items
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist
import java.net.URLClassLoader
import java.util.*


/**
 * Main mod class.
 *
 * An example for blocks is in the `blocks` package of this mod.
 */
@Mod(Strangeteleport.MOD_ID)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object Strangeteleport {
    const val MOD_ID = "strangeteleport"

    // the logger for our mod
    internal val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    init {
        LOGGER.log(Level.INFO, "Hello world!")

        BlockEntities.REGISTRY.register(MOD_BUS)
        // Register the KDeferredRegister to the mod-specific event bus
        Blocks.REGISTRY.register(MOD_BUS)
        Items.REGISTRY.register(MOD_BUS)
        Attachments.REGISTRY.register(MOD_BUS)


        val obj = runForDist(clientTarget = {
            MOD_BUS.addListener(::onClientSetup)
            Minecraft.getInstance()
        }, serverTarget = {
            MOD_BUS.addListener(::onServerSetup)
            "test"
        })

        println(obj)
    }


    /**
     * This is used for initializing client-specific
     * things such as renderers and keymaps
     * Fired on the mod-specific event bus.
     */
    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Initializing client...")
        val cl = ClassLoader.getSystemClassLoader()

        dumpClasspath(cl)
    }

    fun dumpClasspath(loader: ClassLoader) {
        LOGGER.info("Classloader $loader:")

        if (loader is URLClassLoader) {
            LOGGER.info("\t" + loader.urLs.contentToString())
        } else LOGGER.info("\t(cannot display components as not a URLClassLoader)")

        if (loader.parent != null) dumpClasspath(loader.parent)
    }

    /**
     * Fired on the global Forge bus.
     */
    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.log(Level.INFO, "Server starting...")
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.log(Level.INFO, "Hello! This is working!")
    }
}
