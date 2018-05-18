package cofh.thermalcultivation.item;

import cofh.core.init.CoreEnchantments;
import cofh.core.init.CoreProps;
import cofh.core.item.ItemMultiRF;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalcultivation.ThermalCultivation;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class ItemScythe extends ItemMultiRF implements IInitializer {

	public ItemScythe() {

		super("thermalcultivation");

		setUnlocalizedName("scythe");
		setCreativeTab(ThermalCultivation.tabTools);

		setHasSubtypes(true);
		setMaxStackSize(1);
	}

	@Override
	public ItemStack setDefaultTag(ItemStack stack, int energy) {

		EnergyHelper.setDefaultEnergyTag(stack, energy);
		stack.getTagCompound().setInteger(CoreProps.MODE, getNumModes(stack) - 1);

		return stack;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flagIn) {

		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		int radius = getRadius(stack) * 2 + 1;

		tooltip.add(StringHelper.getInfoText("info.thermalcultivation.scythe.a.0"));
		tooltip.add(StringHelper.localize("info.cofh.area") + ": " + radius + "x" + radius);

		if (getNumModes(stack) > 1) {
			tooltip.add(StringHelper.localizeFormat("info.thermalcultivation.scythe.b.0", StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));
		}
		if (isCreative(stack)) {
			tooltip.add(StringHelper.localize("info.cofh.charge") + ": 1.21G RF");
		} else {
			tooltip.add(StringHelper.localize("info.cofh.charge") + ": " + StringHelper.getScaledNumber(getEnergyStored(stack)) + " / " + StringHelper.getScaledNumber(getMaxEnergyStored(stack)) + " RF");
		}
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {

		if (isInCreativeTab(tab)) {
			for (int metadata : itemList) {
				if (metadata != CREATIVE) {
					items.add(setDefaultTag(new ItemStack(this, 1, metadata), 0));
					items.add(setDefaultTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
				} else {
					items.add(setDefaultTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
				}
			}
		}
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {

		if (EnumEnchantmentType.BREAKABLE.equals(enchantment.type)) {
			return enchantment.equals(Enchantments.UNBREAKING);
		} else if (EnumEnchantmentType.DIGGER.equals(enchantment.type)) {
			return enchantment.equals(Enchantments.FORTUNE);
		}
		return !EnumEnchantmentType.BREAKABLE.equals(enchantment.type) && super.canApplyAtEnchantingTable(stack, enchantment);
	}

	@Override
	public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {

		World world = player.world;
		IBlockState state = world.getBlockState(pos);

		// world.playEvent(2001, pos, Block.getStateId(state));
		if (player.isSneaking()) {
			if (!player.capabilities.isCreativeMode) {
				useEnergy(stack, 1, false);
			}
			return false;
		}
		BlockPos adjPos;
		int count = 0;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int radius = getRadius(stack);

		loop:
		for (int i = x - radius; i <= x + radius; i++) {
			for (int k = z - radius; k <= z + radius; k++) {
				adjPos = new BlockPos(i, y, k);
				if (getEnergyStored(stack) < ENERGY_PER_USE * count + 1) {
					break loop;
				}
				if (harvestBlock(world, adjPos, player)) {
					count++;
				}
			}
		}
		if (count > 0 && !player.capabilities.isCreativeMode) {
			useEnergy(stack, count, false);
			return true;
		}
		return false;
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {

		return getEnchantability(stack);
	}

	/* HELPERS */
	protected boolean harvestBlock(World world, BlockPos pos, EntityPlayer player) {

		if (world.isAirBlock(pos)) {
			return false;
		}
		EntityPlayerMP playerMP = null;
		if (player instanceof EntityPlayerMP) {
			playerMP = (EntityPlayerMP) player;
		}
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		// only harvestable plants
		if (!(block instanceof IGrowable)) {
			return false;
		}
		IGrowable grow = (IGrowable) block;
		if (grow.canGrow(world, pos, state, world.isRemote)) {
			return false;
		}
		if (!ForgeHooks.canHarvestBlock(block, player, world, pos)) {
			return false;
		}
		// send the blockbreak event
		int xpToDrop = 0;
		if (playerMP != null) {
			xpToDrop = ForgeHooks.onBlockBreakEvent(world, playerMP.interactionManager.getGameType(), playerMP, pos);
			if (xpToDrop == -1) {
				return false;
			}
		}
		// Creative Mode
		if (player.capabilities.isCreativeMode) {
			if (!world.isRemote) {
				if (block.removedByPlayer(state, world, pos, player, false)) {
					block.onBlockDestroyedByPlayer(world, pos, state);
				}
				// always send block update to client
				playerMP.connection.sendPacket(new SPacketBlockChange(world, pos));
			} else {
				if (block.removedByPlayer(state, world, pos, player, false)) {
					block.onBlockDestroyedByPlayer(world, pos, state);
				}
				Minecraft.getMinecraft().getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, Minecraft.getMinecraft().objectMouseOver.sideHit));
			}
		}
		// Otherwise
		if (!world.isRemote) {
			if (block.removedByPlayer(state, world, pos, player, true)) {
				block.onBlockDestroyedByPlayer(world, pos, state);
				block.harvestBlock(world, player, pos, state, world.getTileEntity(pos), player.getHeldItemMainhand());
				if (xpToDrop > 0) {
					block.dropXpOnBlockBreak(world, pos, xpToDrop);
				}
			}
			// always send block update to client
			playerMP.connection.sendPacket(new SPacketBlockChange(world, pos));
		} else {
			if (block.removedByPlayer(state, world, pos, player, true)) {
				block.onBlockDestroyedByPlayer(world, pos, state);
			}
			Minecraft.getMinecraft().getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, Minecraft.getMinecraft().objectMouseOver.sideHit));
		}
		return true;
	}

	@Override
	protected int getCapacity(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		int capacity = typeMap.get(ItemHelper.getItemDamage(stack)).capacity;
		int enchant = EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack);

		return capacity + capacity * enchant / 2;
	}

	@Override
	protected int getReceive(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).recv;
	}

	protected int useEnergy(ItemStack stack, int count, boolean simulate) {

		if (isCreative(stack)) {
			return 0;
		}
		int unbreakingLevel = MathHelper.clamp(EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack), 0, 10);
		if (MathHelper.RANDOM.nextInt(2 + unbreakingLevel) >= 2) {
			return 0;
		}
		return extractEnergy(stack, count * ENERGY_PER_USE, simulate);
	}

	public int getBaseCapacity(int metadata) {

		if (!typeMap.containsKey(metadata)) {
			return 0;
		}
		return typeMap.get(metadata).capacity;
	}

	public int getEnchantability(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).enchantability;
	}

	public int getMaxRadius(int metadata) {

		if (!typeMap.containsKey(metadata)) {
			return 0;
		}
		return typeMap.get(metadata).radius;
	}

	public int getRadius(ItemStack stack) {

		return 1 + getMode(stack);
	}

	/* IMultiModeItem */
	@Override
	public int getNumModes(ItemStack stack) {

		return getMaxRadius(ItemHelper.getItemDamage(stack));
	}

	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.6F, 1.0F - 0.1F * getMode(stack));

		int radius = getRadius(stack) * 2 + 1;
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.cofh.area").appendText(": " + radius + "x" + radius));
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		ForgeRegistries.ITEMS.register(setRegistryName("scythe"));
		ThermalCultivation.proxy.addIModelRegister(this);

		config();

		scytheBasic = addEntryItem(0, "standard0", ENCHANTABILITY[0], CAPACITY[0], XFER[0], 1, EnumRarity.COMMON);
		scytheHardened = addEntryItem(1, "standard1", ENCHANTABILITY[1], CAPACITY[1], XFER[1], 2, EnumRarity.COMMON);
		scytheReinforced = addEntryItem(2, "standard2", ENCHANTABILITY[2], CAPACITY[2], XFER[2], 3, EnumRarity.UNCOMMON);
		scytheSignalum = addEntryItem(3, "standard3", ENCHANTABILITY[3], CAPACITY[3], XFER[3], 4, EnumRarity.UNCOMMON);
		scytheResonant = addEntryItem(4, "standard4", ENCHANTABILITY[4], CAPACITY[4], XFER[4], 5, EnumRarity.RARE);

		scytheCreative = addEntryItem(CREATIVE, "creative", ENCHANTABILITY[4], CAPACITY[4], 0, 5, EnumRarity.EPIC);

		return true;
	}

	@Override
	public boolean initialize() {

		if (!enable) {
			return false;
		}
		// @formatter:off
//		addShapedRecipe(drillBasic,
//				" X ",
//				"ICI",
//				"RYR",
//				'C', "blockCopper",
//				'I', "gearLead",
//				'R', "dustRedstone",
//				'X', "blockIron",
//				'Y', ItemMaterial.powerCoilGold
//		);
		// @formatter:on
		return true;
	}

	private static void config() {

		String category = "Item.Scythe";
		String comment;

		enable = ThermalCultivation.CONFIG.get(category, "Enable", true);

		int capacity = CAPACITY_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF) stored by a Basic Harvest Scythe. This base value will scale with item level.";
		capacity = ThermalCultivation.CONFIG.getConfiguration().getInt("BaseCapacity", category, capacity, CAPACITY_MIN, CAPACITY_MAX, comment);

		int xfer = XFER_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF/t) that can be received by a Basic Harvest Scythe. This base value will scale with item level.";
		xfer = ThermalCultivation.CONFIG.getConfiguration().getInt("BaseReceive", category, xfer, XFER_MIN, XFER_MAX, comment);

		for (int i = 0; i < CAPACITY.length; i++) {
			CAPACITY[i] *= capacity;
			XFER[i] *= xfer;
		}
	}

	/* ENTRY */
	public class TypeEntry {

		public final String name;

		public final int enchantability;

		public final int capacity;
		public final int recv;
		public final int radius;

		TypeEntry(String name, int enchantability, int capacity, int recv, int radius) {

			this.name = name;
			this.enchantability = enchantability;
			this.capacity = capacity;
			this.recv = recv;
			this.radius = radius;
		}
	}

	private void addEntry(int metadata, String name, int enchantability, int capacity, int xfer, int radius) {

		typeMap.put(metadata, new TypeEntry(name, enchantability, capacity, xfer, radius));
	}

	private ItemStack addEntryItem(int metadata, String name, int enchantability, int capacity, int xfer, int radius, EnumRarity rarity) {

		addEntry(metadata, name, enchantability, capacity, xfer, radius);
		return addItem(metadata, name, rarity);
	}

	private static Int2ObjectOpenHashMap<TypeEntry> typeMap = new Int2ObjectOpenHashMap<>();

	public static final int CAPACITY_BASE = 20000;
	public static final int XFER_BASE = 1000;
	public static final int ENERGY_PER_USE = 50;

	public static final int[] ENCHANTABILITY = { 10, 10, 15, 15, 20 };

	public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };
	public static final int[] XFER = { 1, 4, 9, 16, 25 };

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack scytheBasic;
	public static ItemStack scytheHardened;
	public static ItemStack scytheReinforced;
	public static ItemStack scytheSignalum;
	public static ItemStack scytheResonant;

	public static ItemStack scytheCreative;

}
