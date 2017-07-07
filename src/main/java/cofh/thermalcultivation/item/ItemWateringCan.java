package cofh.thermalcultivation.item;

import cofh.api.fluid.IFluidContainerItem;
import cofh.api.item.IMultiModeItem;
import cofh.core.init.CoreEnchantments;
import cofh.core.item.IEnchantableItem;
import cofh.core.item.ItemMulti;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.RayTracer;
import cofh.core.util.capabilities.FluidContainerItemWrapper;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalcultivation.ThermalCultivation;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.core.util.helpers.RecipeHelper.addShapedRecipe;

public class ItemWateringCan extends ItemMulti implements IInitializer, IFluidContainerItem, IMultiModeItem, IEnchantableItem {

	public ItemWateringCan() {

		super("thermalcultivation");

		setUnlocalizedName("watering_can");
		setCreativeTab(ThermalCultivation.tabCommon);

		setHasSubtypes(true);
		setMaxStackSize(1);
	}

	protected static boolean isWater(IBlockState state) {

		return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER) && state.getValue(BlockLiquid.LEVEL) == 0;
	}

	public ItemStack setDefaultTag(ItemStack stack, int water) {

		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("Water", water);
		stack.getTagCompound().setInteger("Mode", getNumModes(stack) - 1);

		return stack;
	}

	public int getBaseCapacity(int metadata) {

		if (!typeMap.containsKey(metadata)) {
			return 0;
		}
		return typeMap.get(metadata).capacity;
	}

	public int getChance(int metadata) {

		if (!typeMap.containsKey(metadata)) {
			return 0;
		}
		return typeMap.get(metadata).chance;
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

	public int getSpace(ItemStack stack) {

		if (stack.getTagCompound() == null) {
			setDefaultTag(stack, 0);
		}
		return getCapacity(stack) - getWaterStored(stack);
	}

	public int getWaterStored(ItemStack stack) {

		if (stack.getTagCompound() == null) {
			setDefaultTag(stack, 0);
		}
		return stack.getTagCompound().getInteger("Water");
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		int metadata = ItemHelper.getItemDamage(stack);
		int radius = getRadius(stack) * 2 + 1;

		tooltip.add(StringHelper.getInfoText("info.thermalcultivation.watering_can.0"));
		tooltip.add(StringHelper.localize("info.cofh.area") + ": " + radius + "x" + radius);

		if (getNumModes(stack) > 1) {
			tooltip.add(StringHelper.localizeFormat("info.thermalcultivation.watering_can.1", StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));
		}
		if (metadata == CREATIVE) {
			tooltip.add(StringHelper.localize(FluidRegistry.WATER.getUnlocalizedName()) + ": " + StringHelper.localize("info.cofh.infinite"));
		} else {
			tooltip.add(StringHelper.localize(StringHelper.localize(FluidRegistry.WATER.getUnlocalizedName()) + ": " + StringHelper.formatNumber(getWaterStored(stack)) + " / " + StringHelper.formatNumber(getCapacity(stack)) + " mB"));
			tooltip.add(StringHelper.getNoticeText("info.thermalcultivation.watering_can.2"));
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
	public boolean isDamaged(ItemStack stack) {

		return true;
	}

	@Override
	public boolean isFull3D() {

		return true;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {

		return true;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

		return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged) && (slotChanged || !ItemHelper.areItemStacksEqualIgnoreTags(oldStack, newStack, "Water"));
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {

		return ItemHelper.getItemDamage(stack) != CREATIVE;
	}

	@Override
	public int getItemEnchantability() {

		return 10;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {

		if (stack.getTagCompound() == null) {
			EnergyHelper.setDefaultEnergyTag(stack, 0);
		}
		return 1D - ((double) stack.getTagCompound().getInteger("Water") / (double) getCapacity(stack));
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {

		RayTraceResult traceResult = RayTracer.retrace(player, true);
		BlockPos tracePos = traceResult.getBlockPos();
		ItemStack stack = player.getHeldItem(hand);

		if (!player.isSneaking() || !world.isBlockModifiable(player, tracePos)) {
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}
		if (isWater(world.getBlockState(tracePos)) && getSpace(stack) > 0) {
			world.setBlockState(tracePos, Blocks.AIR.getDefaultState(), 11);
			fill(stack, new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME), true);
			player.playSound(SoundEvents.ITEM_BUCKET_FILL, 1.0F, 1.0F);
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}
		return new ActionResult<>(EnumActionResult.PASS, stack);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		RayTraceResult traceResult = RayTracer.retrace(player, true);
		BlockPos tracePos = traceResult.getBlockPos();

		if (player.isSneaking() && isWater(world.getBlockState(tracePos))) {
			return EnumActionResult.FAIL;
		}
		ItemStack stack = player.getHeldItem(hand);
		if (!player.canPlayerEdit(pos.offset(facing), facing, stack) || !player.capabilities.isCreativeMode || getWaterStored(stack) < WATER_PER_USE[0]) {
			return EnumActionResult.FAIL;
		}
		if (player instanceof FakePlayer) {
			return EnumActionResult.FAIL;
		}
		int radius = getRadius(stack);

		int x = pos.getX();
		double y = pos.getY() + 1.3D;
		int z = pos.getZ();

		for (int i = x - radius; i <= x + radius; i++) {
			for (int k = z - radius; k <= z + radius; k++) {
				world.spawnParticle(EnumParticleTypes.WATER_DROP, i + world.rand.nextFloat(), y, k + world.rand.nextFloat(), 0.0D, 0.0D, 0.0D);
			}
		}

		Iterable<BlockPos.MutableBlockPos> area = BlockPos.getAllInBoxMutable(pos.add(-radius, -1, -radius), pos.add(radius, 1, radius));
		for (BlockPos scan : area) {
			IBlockState state = world.getBlockState(scan);

			if (state.getBlock() instanceof BlockFarmland) {
				int moisture = state.getValue(BlockFarmland.MOISTURE);
				if (moisture < 7) {
					world.setBlockState(scan, state.withProperty(BlockFarmland.MOISTURE, 7), 2);
				}
			}
		}
		if (ServerHelper.isServerWorld(world)) {
			if (world.rand.nextInt(100) < getChance(ItemHelper.getItemDamage(stack))) {
				for (BlockPos scan : area) {
					Block plant = world.getBlockState(scan).getBlock();
					if (plant instanceof IGrowable || plant instanceof IPlantable || plant == Blocks.MYCELIUM || plant == Blocks.CHORUS_FLOWER) {
						world.scheduleBlockUpdate(scan, plant, 0, 1);
					}
				}
			}
			if (!player.capabilities.isCreativeMode) {
				drain(stack, WATER_PER_USE[getMode(stack)], true);
			}
		}
		return EnumActionResult.FAIL;
	}

	/* IMultiModeItem */
	@Override
	public int getMode(ItemStack stack) {

		return !stack.hasTagCompound() ? 0 : stack.getTagCompound().getInteger("Mode");
	}

	@Override
	public boolean setMode(ItemStack stack, int mode) {

		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("Mode", mode);
		return false;
	}

	@Override
	public boolean incrMode(ItemStack stack) {

		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		int curMode = getMode(stack);
		curMode++;
		if (curMode >= getNumModes(stack)) {
			curMode = 0;
		}
		stack.getTagCompound().setInteger("Mode", curMode);
		return true;
	}

	@Override
	public boolean decrMode(ItemStack stack) {

		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		int curMode = getMode(stack);
		curMode--;
		if (curMode <= 0) {
			curMode = getNumModes(stack) - 1;
		}
		stack.getTagCompound().setInteger("Mode", curMode);
		return true;
	}

	@Override
	public int getNumModes(ItemStack stack) {

		return getMaxRadius(ItemHelper.getItemDamage(stack));
	}

	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0F, 1.0F - 0.1F * getMode(stack));

		int radius = getRadius(stack) * 2 + 1;
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentString(StringHelper.localize("info.cofh.area") + ": " + radius + "x" + radius));
	}

	/* IFluidContainerItem */
	@Override
	public FluidStack getFluid(ItemStack container) {

		if (container.getTagCompound() == null) {
			setDefaultTag(container, 0);
		}
		int stored = container.getTagCompound().getInteger("Water");
		return new FluidStack(FluidRegistry.WATER, stored);
	}

	@Override
	public int getCapacity(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		int capacity = typeMap.get(ItemHelper.getItemDamage(stack)).capacity;
		int enchant = EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack);

		return capacity + capacity * enchant / 2;
	}

	@Override
	public int fill(ItemStack container, FluidStack resource, boolean doFill) {

		if (container.getTagCompound() == null) {
			setDefaultTag(container, 0);
		}
		if (resource == null || !FluidRegistry.WATER.equals(resource.getFluid())) {
			return 0;
		}
		int stored = container.getTagCompound().getInteger("Water");
		int fill = Math.min(resource.amount, getCapacity(container) - stored);

		if (doFill && ItemHelper.getItemDamage(container) != CREATIVE) {
			stored += fill;
			container.getTagCompound().setInteger("Water", stored);
		}
		return fill;
	}

	@Override
	public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {

		if (container.getTagCompound() == null) {
			setDefaultTag(container, 0);
		}
		if (maxDrain == 0) {
			return null;
		}
		int stored = container.getTagCompound().getInteger("Water");
		int drain = Math.min(maxDrain, stored);

		if (doDrain && ItemHelper.getItemDamage(container) != CREATIVE) {
			stored -= drain;
			container.getTagCompound().setInteger("Water", stored);
		}
		return new FluidStack(FluidRegistry.WATER, drain);
	}

	/* IEnchantableItem */
	@Override
	public boolean canEnchant(ItemStack stack, Enchantment enchantment) {

		return enchantment == CoreEnchantments.holding;
	}

	/* CAPABILITIES */
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {

		return new FluidContainerItemWrapper(stack, this);
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		config();

		wateringCanBasic = addEntryItem(0, "standard0", CAPACITY[0], CHANCE[0], 1, EnumRarity.COMMON);
		wateringCanHardened = addEntryItem(1, "standard1", CAPACITY[1], CHANCE[1], 2, EnumRarity.COMMON);
		wateringCanReinforced = addEntryItem(2, "standard2", CAPACITY[2], CHANCE[2], 3, EnumRarity.UNCOMMON);
		wateringCanSignalum = addEntryItem(3, "standard3", CAPACITY[3], CHANCE[3], 4, EnumRarity.UNCOMMON);
		wateringCanResonant = addEntryItem(4, "standard4", CAPACITY[4], CHANCE[4], 5, EnumRarity.RARE);

		wateringCanCreative = addEntryItem(CREATIVE, "creative", CAPACITY[4], 100, 5, EnumRarity.EPIC, false);

		ThermalCultivation.proxy.addIModelRegister(this);

		return true;
	}

	@Override
	public boolean register() {

		if (!enable) {
			return false;
		}
		// @formatter:off

	addShapedRecipe(wateringCanBasic,
				"I  ",
				"IXI",
				" I ",
				'I', "ingotCopper",
				'X', Items.BUCKET
		);

		// @formatter:on

		return true;
	}

	private static void config() {

		String category = "Item.WateringCan";
		enable = ThermalCultivation.CONFIG.get(category, "Enable", true);

		int capacity = CAPACITY_BASE;
		String comment = "Adjust this value to change the amount of Water (in mB) stored by a Basic Watering Can. This base value will scale with item level.";
		capacity = ThermalCultivation.CONFIG.getConfiguration().getInt("BaseCapacity", category, capacity, capacity / 5, capacity * 5, comment);

		for (int i = 0; i < CAPACITY.length; i++) {
			CAPACITY[i] *= capacity;
		}
	}

	/* ENTRY */
	public class TypeEntry {

		public final String name;
		public final int capacity;
		public final int chance;
		public final int radius;
		public final boolean enchantable;

		TypeEntry(String name, int capacity, int chance, int radius, boolean enchantable) {

			this.name = name;
			this.capacity = capacity;
			this.chance = chance;
			this.radius = radius;
			this.enchantable = enchantable;
		}
	}

	private void addEntry(int metadata, String name, int capacity, int chance, int radius, boolean enchantable) {

		typeMap.put(metadata, new TypeEntry(name, capacity, chance, radius, enchantable));
	}

	private ItemStack addEntryItem(int metadata, String name, int capacity, int chance, int radius, EnumRarity rarity, boolean enchantable) {

		addEntry(metadata, name, capacity, chance, radius, enchantable);
		return addItem(metadata, name, rarity);
	}

	private ItemStack addEntryItem(int metadata, String name, int capacity, int chance, int radius, EnumRarity rarity) {

		addEntry(metadata, name, capacity, chance, radius, true);
		return addItem(metadata, name, rarity);
	}

	private static TIntObjectHashMap<TypeEntry> typeMap = new TIntObjectHashMap<>();

	public static final int CAPACITY_BASE = 4000;
	public static final int CREATIVE = 32000;

	public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };
	public static final int[] CHANCE = { 20, 25, 30, 35, 40 };
	public static final int[] WATER_PER_USE = { 50, 100, 150, 200, 250 };

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack wateringCanBasic;
	public static ItemStack wateringCanHardened;
	public static ItemStack wateringCanReinforced;
	public static ItemStack wateringCanSignalum;
	public static ItemStack wateringCanResonant;

	public static ItemStack wateringCanCreative;

}
