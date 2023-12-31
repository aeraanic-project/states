package eu.ansquare.states.block;

import eu.ansquare.states.StatemakerScreenHandler;
import eu.ansquare.states.States;
import eu.ansquare.states.cca.StatesChunkComponents;
import eu.ansquare.states.cca.StatesEntityComponents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;

import java.util.*;

public class StateBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, Inventory {
	public static void itemStackLoad(ServerPlayerEntity player, BlockPos pos, int i){
		States.LOGGER.info("all");
	}
	public Set<ChunkPos> list = new HashSet<>();
	public NbtList allows = new NbtList();
	public NbtList denys = new NbtList();
	public NbtList tps = new NbtList();

	public UUID uuid = UUID.randomUUID();
	public UUID owner = uuid;
	public String ownername = "";
	public DefaultedList<ItemStack> inventory = DefaultedList.ofSize(4, ItemStack.EMPTY);
	public StateBlockEntity(BlockPos pos, BlockState state) {
		super(StatesBlocks.STATE_BLOCK_ENTITY, pos, state);
	}
	private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
		@Override
		public int get(int index) {
			switch (index){
				case 0:
					return list.size();
				case 1:
					return allows.size();
				case 2:
					return denys.size();
				case 3:
					return tps.size();
			}
			return 0;
		}

		@Override
		public void set(int index, int value) {
		}

		//this is supposed to return the amount of integers you have in your delegate, in our example only one
		@Override
		public int size() {
			return 4;
		}
	};

	public void init(UUID owner, ChunkPos pos){
		StatesChunkComponents.CLAIMED_CHUNK_COMPONENT.maybeGet(world.getChunk(pos.x, pos.z)).ifPresent(claimedChunkComponent -> {
			if(claimedChunkComponent.claim(this).print(pos).valid){
				list.add(pos);
			}
		});
		this.owner = owner;
		allows.add(allows.size(), NbtHelper.fromUuid(owner));
		StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(owner)).ifPresent(citizenComponent -> citizenComponent.addAllow(uuid));
		ownername = getWorld().getServer().getPlayerManager().getPlayer(owner).getDisplayName().getString();
	}
	@Override
	public void writeNbt(NbtCompound nbt) {
		// Save the current value of the number to the nbt
		LinkedList<Integer> xs = new LinkedList<>();
		LinkedList<Integer> zs = new LinkedList<>();
		list.forEach(chunkPos -> {
			xs.addLast(chunkPos.x);
			zs.addLast(chunkPos.z);
		});
		nbt.putString("ownername", ownername);
		nbt.put("allow", allows);
		nbt.put("denys", denys);
		nbt.put("tps", tps);
		nbt.putUuid("owner", owner);
		nbt.putIntArray("xs", xs);
		nbt.putIntArray("zs", zs);
		nbt.putUuid("stateid", uuid);
		Inventories.writeNbt(nbt, inventory);
		super.writeNbt(nbt);
	}
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		ownername = nbt.getString("ownername");
		allows = nbt.getList("allow", 11);
		denys = nbt.getList("denys", 11);
		tps = nbt.getList("tps", 11);
		owner = nbt.getUuid("owner");
		inventory = DefaultedList.ofSize(4, ItemStack.EMPTY);
		Inventories.readNbt(nbt, this.inventory);
		int[] xs = nbt.getIntArray("xs");
		int[] zs = nbt.getIntArray("zs");
		uuid = nbt.getUuid("stateid");
		for (int i = 0; i < xs.length; i++){
			list.add(new ChunkPos(xs[i], zs[i]));
		}
	}
	public void addChunksFromNbtList(NbtList nbtlist){
		for (int i = 0; i < nbtlist.size(); i++) {
			int[] array = nbtlist.getIntArray(i);
			ChunkPos pos = new ChunkPos(array[0], array[1]);
			StatesChunkComponents.CLAIMED_CHUNK_COMPONENT.maybeGet(world.getChunk(pos.x, pos.z)).ifPresent(claimedChunkComponent -> {
				if(claimedChunkComponent.claim(this).print(pos).valid){
					list.add(pos);
				}
			});
		}
	}
	public boolean canTp(UUID uuid){
		NbtElement element = NbtHelper.fromUuid(uuid);

		return allows.contains(element) || tps.contains(element);
	}
	public void addPlayers(NbtList list, int i){
		if (i == 1){
			list.forEach(nbtElement -> {
				UUID uuid1 = NbtHelper.toUuid(nbtElement);
				if(!allows.contains(nbtElement) && !denys.contains(nbtElement)){
					StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(uuid1)).ifPresent(citizenComponent -> citizenComponent.addAllow(uuid));
					allows.add(allows.size(), nbtElement);

				}
			});
		} else if (i == 2){
			list.forEach(nbtElement -> {
				UUID uuid1 = NbtHelper.toUuid(nbtElement);
				if(!allows.contains(nbtElement) && !denys.contains(nbtElement)){
					StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(uuid1)).ifPresent(citizenComponent -> citizenComponent.addDeny(uuid));
					denys.add(denys.size(), nbtElement);
				}
			});
		} else if(i == 3){
			list.forEach(nbtElement -> {
				UUID uuid1 = NbtHelper.toUuid(nbtElement);
				if(!allows.contains(nbtElement) && !tps.contains(nbtElement)){
					StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(uuid1)).ifPresent(citizenComponent -> citizenComponent.addTp(uuid));
					tps.add(tps.size(), nbtElement);
				}
			});
		}
	}
	public void destroy(){
		list.forEach(chunkPos -> StatesChunkComponents.CLAIMED_CHUNK_COMPONENT.maybeGet(world.getChunk(chunkPos.x, chunkPos.z)).ifPresent(claimedChunkComponent -> claimedChunkComponent.unclaim()));
		list.clear();
		allows.forEach(nbtElement -> StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(NbtHelper.toUuid(nbtElement))).ifPresent(citizenComponent -> citizenComponent.removeAllow(uuid)));
		allows.clear();
		denys.forEach(nbtElement -> StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(NbtHelper.toUuid(nbtElement))).ifPresent(citizenComponent -> citizenComponent.removeDeny(uuid)));
		denys.clear();
		tps.forEach(nbtElement -> StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(NbtHelper.toUuid(nbtElement))).ifPresent(citizenComponent -> citizenComponent.removeTp(uuid)));
		tps.clear();
	}
	public void loadFromStack(int stackint){
		ItemStack stack = inventory.get(stackint);
		States.LOGGER.info(stack.toString());
		if(stackint == 0){
			NbtList list1 = stack.getOrCreateNbt().getList("chunks", 11);
			addChunksFromNbtList(list1);
			markDirty();
		} else {
			NbtList list1 = stack.getOrCreateNbt().getList("players", 11);
			addPlayers(list1, stackint);
			markDirty();
		}
	}
	public void clear(int stackint){
		if(stackint == 0){
			list.forEach(chunkPos -> {
					StatesChunkComponents.CLAIMED_CHUNK_COMPONENT.maybeGet(world.getChunk(chunkPos.x, chunkPos.z)).ifPresent(claimedChunkComponent -> claimedChunkComponent.unclaim());
			});
			list.clear();
			StatesChunkComponents.CLAIMED_CHUNK_COMPONENT.maybeGet(getWorld().getChunk(getPos())).ifPresent(claimedChunkComponent -> {
				if(claimedChunkComponent.claim(this).valid){
					list.add(getWorld().getChunk(getPos()).getPos());
				}
			});
		} else if (stackint == 1) {
			allows.forEach(nbtElement -> {
				UUID uuid1 = NbtHelper.toUuid(nbtElement);
				StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(uuid1)).ifPresent(citizenComponent -> citizenComponent.removeAllow(uuid));
			});
			allows.clear();
			allows.add(allows.size(), NbtHelper.fromUuid(owner));
			StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(getWorld().getServer().getPlayerManager().getPlayer(owner)).ifPresent(citizenComponent -> citizenComponent.addAllow(uuid));

		} else if (stackint == 2) {
			denys.forEach(nbtElement -> {
				UUID uuid1 = NbtHelper.toUuid(nbtElement);
				StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(uuid1).ifPresent(citizenComponent -> citizenComponent.removeDeny(uuid));
			});
			denys.clear();
		}else if (stackint == 3) {
			tps.forEach(nbtElement -> {
				UUID uuid1 = NbtHelper.toUuid(nbtElement);
				StatesEntityComponents.CITIZEN_COMPONENT.maybeGet(uuid1).ifPresent(citizenComponent -> citizenComponent.removeTp(uuid));
			});
			tps.clear();
		}
	}

	@Override
	public Text getDisplayName() {
		return Text.translatable("screen.states.title", new Object[]{ownername});
	}

	@Nullable
	@Override
	public ScreenHandler createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
		return new StatemakerScreenHandler(i, playerInventory, owner, this, this.getPos(), propertyDelegate);
	}

	@Override
	public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
		buf.writeUuid(owner).writeBlockPos(this.getPos());
	}
	@Override
	public int size() {
		return 4;
	}

	@Override
	public boolean isEmpty() {
		return this.inventory.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getStack(int slot) {
		return inventory.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack itemStack = Inventories.splitStack(this.inventory, slot, amount);
		if (!itemStack.isEmpty()) {
			this.markDirty();
		}

		return itemStack;
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(inventory, slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		this.inventory.set(slot, stack);
		if (stack.getCount() > this.getMaxCountPerStack()) {
			stack.setCount(this.getMaxCountPerStack());
		}

		this.markDirty();

	}

	public boolean canPlayerUse(PlayerEntity player) {
		return Inventory.canPlayerUse(this, player);
	}


	@Override
	public void clear() {
		inventory.clear();
	}
}
