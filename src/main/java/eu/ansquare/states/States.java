package eu.ansquare.states;

import eu.ansquare.states.block.StatesBlocks;
import eu.ansquare.states.item.StatesItems;
import eu.ansquare.states.network.StatesNetwork;
import eu.ansquare.states.api.StatePermission;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.command.argument.EntityArgumentType.player;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class States implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod name as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("States");
	public static final String MODID = "states";
	public static final ExtendedScreenHandlerType<StatemakerScreenHandler> STATEMAKER_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(StatemakerScreenHandler::new);
	public static final TagKey<Item> STATEMAKERS = TagKey.of(RegistryKeys.ITEM, new Identifier(MODID, "statemakers"));
	public static final GameRules.Key<GameRules.BooleanRule> CAN_TELEPORT_TO_STATES =
			GameRuleRegistry.register("canTpToStates", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("Hello Quilt world from {}!", mod.metadata().name());

		StatesBlocks.init();
		StatesItems.init();
		StatesNetwork.initC2S();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> dispatcher.register(literal("state")
				.then(argument("player", player())
						.executes(context -> {
							PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
							StatePermission permission = StatePermission.permissionAt(player.getWorld().getChunk(player.getBlockPos()), player);
							context.getSource().getPlayer().sendMessage(permission.result, false);
			return 1;
		}))));
		StatesEvents.init();
		Registry.register(Registries.SCREEN_HANDLER_TYPE, new Identifier(MODID, "state_screen"), STATEMAKER_SCREEN_HANDLER);
	}
}
