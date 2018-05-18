package cofh.thermalcultivation.item;

import cofh.api.fluid.IFluidContainerItem;
import cofh.api.item.IColorableItem;
import cofh.api.item.IMultiModeItem;
import cofh.core.init.CoreEnchantments;
import cofh.core.init.CoreProps;
import cofh.core.item.IEnchantableItem;
import cofh.core.item.ItemMulti;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.RayTracer;
import cofh.core.util.capabilities.FluidContainerItemWrapper;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalcultivation.ThermalCultivation;
import cofh.thermalfoundation.init.TFProps;
import cofh.thermalfoundation.item.ItemFertilizer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static cofh.core.util.helpers.RecipeHelper.*;

public class ItemWateringCan extends ItemMulti implements IInitializer, IColorableItem, IEnchantableItem, IFluidContainerItem, IMultiModeItem {

	public ItemWateringCan() {

		super("thermalcultivation");

		setUnlocalizedName("watering_can");
		setCreativeTab(ThermalCultivation.tabTools);

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
		stack.getTagCompound().setInteger(CoreProps.WATER, water);
		stack.getTagCompound().setInteger(CoreProps.MODE, getNumModes(stack) - 1);

		return stack;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		int radius = getRadius(stack) * 2 + 1;

		tooltip.add(StringHelper.getInfoText("info.thermalcultivation.watering_can.a.0"));
		tooltip.add(StringHelper.localize("info.cofh.area") + ": " + radius + "x" + radius);

		if (getNumModes(stack) > 1) {
			tooltip.add(StringHelper.localizeFormat("info.thermalcultivation.watering_can.b.0", StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));
		}
		if (isCreative(stack)) {
			tooltip.add(StringHelper.localize(FluidRegistry.WATER.getUnlocalizedName()) + ": " + StringHelper.localize("info.cofh.infinite"));
		} else {
			tooltip.add(StringHelper.localize(StringHelper.localize(FluidRegistry.WATER.getUnlocalizedName()) + ": " + StringHelper.formatNumber(getWaterStored(stack)) + " / " + StringHelper.formatNumber(getCapacity(stack)) + " mB"));
			tooltip.add(StringHelper.getNoticeText("info.thermalcultivation.watering_can.c.0"));
		}
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {

		if (enable && isInCreativeTab(tab)) {
			for (int metadata : itemList) {
				if (metadata != CREATIVE) {
					if (TFProps.showEmptyItems) {
						items.add(setDefaultTag(new ItemStack(this, 1, metadata), 0));
					}
					if (TFProps.showFullItems) {
						items.add(setDefaultTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
					}
				} else {
					if (TFProps.showCreativeItems) {
						items.add(setDefaultTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
					}
				}
			}
		}
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isSelected) {

		if (!isActive(stack)) {
			return;
		}
		long activeTime = stack.getTagCompound().getLong(CoreProps.ACTIVE);

		if (entity.world.getTotalWorldTime() > activeTime) {
			stack.getTagCompound().removeTag(CoreProps.ACTIVE);
		}
	}

	@Override
	public boolean isFull3D() {

		return true;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {

		return !isCreative(stack);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

		return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged) && (slotChanged || ItemHelper.getItemDamage(oldStack) != ItemHelper.getItemDamage(newStack) || getWaterStored(oldStack) > 0 != getWaterStored(newStack) > 0 || getWaterStored(newStack) > 0 && isActive(oldStack) != isActive(newStack));
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {

		return !isCreative(stack) && getWaterStored(stack) > 0;
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {

		return 10;
	}

	@Override
	public int getRGBDurabilityForDisplay(ItemStack stack) {

		return CoreProps.RGB_DURABILITY_WATER;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {

		if (stack.getTagCompound() == null) {
			setDefaultTag(stack, 0);
		}
		return 1.0D - ((double) stack.getTagCompound().getInteger(CoreProps.WATER) / (double) getCapacity(stack));
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {

		RayTraceResult traceResult = RayTracer.retrace(player, true);
		ItemStack stack = player.getHeldItem(hand);

		if (traceResult == null || traceResult.sideHit == null) {
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}
		BlockPos tracePos = traceResult.getBlockPos();

		if (!player.isSneaking() || !world.isBlockModifiable(player, tracePos) || player instanceof FakePlayer && !allowFakePlayers) {
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}
		if (isWater(world.getBlockState(tracePos)) && getSpace(stack) > 0) {
			if (removeSourceBlocks) {
				world.setBlockState(tracePos, Blocks.AIR.getDefaultState(), 11);
			}
			fill(stack, new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME), true);
			player.playSound(SoundEvents.ITEM_BUCKET_FILL, 1.0F, 1.0F);
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}
		return new ActionResult<>(EnumActionResult.PASS, stack);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		RayTraceResult traceResult = RayTracer.retrace(player, true);

		if (traceResult == null || traceResult.sideHit == null || player.isSneaking() || player instanceof FakePlayer && !allowFakePlayers) {
			return EnumActionResult.FAIL;
		}
		ItemStack stack = player.getHeldItem(hand);
		BlockPos tracePos = traceResult.getBlockPos();
		IBlockState traceState = world.getBlockState(tracePos);
		//BlockPos offsetPos = traceState.isSideSolid(world, tracePos, traceResult.sideHit) /*|| !traceState.isFullCube()*/ || traceState.getMaterial().isLiquid() ? tracePos.offset(traceResult.sideHit) : tracePos;
		BlockPos offsetPos = tracePos.offset(traceResult.sideHit);

		if (getWaterStored(stack) < WATER_PER_USE[0]) {
			return EnumActionResult.FAIL;
		}
		setActive(stack, player);

		int radius = getRadius(stack);
		int x = offsetPos.getX();
		double y = offsetPos.getY() + 0.4D;
		int z = offsetPos.getZ();

		for (int i = x - radius; i <= x + radius; i++) {
			for (int k = z - radius; k <= z + radius; k++) {
				world.spawnParticle(EnumParticleTypes.WATER_DROP, i + world.rand.nextFloat(), y, k + world.rand.nextFloat(), 0.0D, 0.0D, 0.0D);
			}
		}
		Iterable<BlockPos.MutableBlockPos> area = BlockPos.getAllInBoxMutable(offsetPos.add(-radius, -1, -radius), offsetPos.add(radius, 1, radius));
		for (BlockPos scan : area) {
			IBlockState state = world.getBlockState(scan);

			if (state.getBlock() instanceof BlockFarmland) {
				int moisture = state.getValue(BlockFarmland.MOISTURE);
				if (moisture < 7) {
					world.setBlockState(scan, state.withProperty(BlockFarmland.MOISTURE, 7), 3);
				}
			}
		}
		if (ServerHelper.isServerWorld(world)) {
			if (world.rand.nextInt(100) < getChance(ItemHelper.getItemDamage(stack)) - 5 * getMode(stack)) {
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

	/* HELPERS */
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
		return stack.getTagCompound().getInteger(CoreProps.WATER);
	}

	public void setActive(ItemStack stack, EntityPlayer player) {

		stack.getTagCompound().setLong(CoreProps.ACTIVE, player.world.getTotalWorldTime() + 10);
	}

	/* IEnchantableItem */
	@Override
	public boolean canEnchant(ItemStack stack, Enchantment enchantment) {

		return !isCreative(stack) && enchantment == CoreEnchantments.holding;
	}

	/* IFluidContainerItem */
	@Override
	public FluidStack getFluid(ItemStack container) {

		if (container.getTagCompound() == null) {
			setDefaultTag(container, 0);
		}
		int stored = container.getTagCompound().getInteger(CoreProps.WATER);
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
		if (resource == null || resource.amount <= 0 || !FluidRegistry.WATER.equals(resource.getFluid())) {
			return 0;
		}
		int stored = container.getTagCompound().getInteger(CoreProps.WATER);
		int fill = Math.min(resource.amount, getCapacity(container) - stored);

		if (doFill && !isCreative(container)) {
			stored += fill;
			container.getTagCompound().setInteger(CoreProps.WATER, stored);
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
		if (isCreative(container)) {
			return new FluidStack(FluidRegistry.WATER, maxDrain);
		}
		int stored = container.getTagCompound().getInteger(CoreProps.WATER);
		int drain = Math.min(maxDrain, stored);

		if (doDrain) {
			stored -= drain;
			container.getTagCompound().setInteger(CoreProps.WATER, stored);
		}
		return new FluidStack(FluidRegistry.WATER, drain);
	}

	/* IMultiModeItem */
	@Override
	public int getNumModes(ItemStack stack) {

		return getMaxRadius(ItemHelper.getItemDamage(stack));
	}

	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 0.6F, 1.0F - 0.1F * getMode(stack));

		int radius = getRadius(stack) * 2 + 1;
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.cofh.area").appendText(": " + radius + "x" + radius));
	}

	/* CAPABILITIES */
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {

		return new FluidContainerItemWrapper(stack, this);
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		ModelLoader.setCustomMeshDefinition(this, stack -> new ModelResourceLocation(getRegistryName(), String.format("color0=%s,type=%s,water=%s", ColorHelper.hasColor0(stack) ? 1 : 0, typeMap.get(ItemHelper.getItemDamage(stack)).name, this.getWaterStored(stack) > 0 ? isActive(stack) ? "tipped" : "level" : "empty")));

		String[] states = { "level", "tipped", "empty" };

		for (Map.Entry<Integer, ItemEntry> entry : itemMap.entrySet()) {
			for (int color0 = 0; color0 < 2; color0++) {
				for (int state = 0; state < 3; state++) {
					ModelBakery.registerItemVariants(this, new ModelResourceLocation(getRegistryName(), String.format("color0=%s,type=%s,water=%s", color0, entry.getValue().name, states[state])));
				}
			}
		}
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		ForgeRegistries.ITEMS.register(setRegistryName("watering_can"));
		ThermalCultivation.proxy.addIModelRegister(this);

		config();

		wateringCanBasic = addEntryItem(0, "standard0", CAPACITY[0], CHANCE[0], 1, EnumRarity.COMMON);
		wateringCanHardened = addEntryItem(1, "standard1", CAPACITY[1], CHANCE[1], 2, EnumRarity.COMMON);
		wateringCanReinforced = addEntryItem(2, "standard2", CAPACITY[2], CHANCE[2], 3, EnumRarity.UNCOMMON);
		wateringCanSignalum = addEntryItem(3, "standard3", CAPACITY[3], CHANCE[3], 4, EnumRarity.UNCOMMON);
		wateringCanResonant = addEntryItem(4, "standard4", CAPACITY[4], CHANCE[4], 5, EnumRarity.RARE);

		wateringCanCreative = addEntryItem(CREATIVE, "creative", CAPACITY[4], CHANCE_CREATIVE, 5, EnumRarity.EPIC);

		return true;
	}

	@Override
	public boolean initialize() {

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
		addShapedUpgradeRecipe(wateringCanHardened,
				" R ",
				"IXI",
				"RYR",
				'I', "ingotInvar",
				'R', new ItemStack(Items.DYE, 1, 15),
				'X', wateringCanBasic,
				'Y', "dustRedstone"
		);
		addShapedUpgradeRecipe(wateringCanReinforced,
				" R ",
				"IXI",
				"RYR",
				'I', "ingotElectrum",
				'R', ItemFertilizer.fertilizerBasic,
				'X', wateringCanHardened,
				'Y', "blockGlassHardened"
		);
		addShapedUpgradeRecipe(wateringCanSignalum,
				" R ",
				"IXI",
				"RYR",
				'I', "ingotSignalum",
				'R', ItemFertilizer.fertilizerRich,
				'X', wateringCanReinforced,
				'Y', "dustCryotheum"
		);
		addShapedUpgradeRecipe(wateringCanResonant,
				" R ",
				"IXI",
				"RYR",
				'I', "ingotEnderium",
				'R', ItemFertilizer.fertilizerFlux,
				'X', wateringCanSignalum,
				'Y', "dustPyrotheum"
		);
		// @formatter:on

		addColorRecipe(wateringCanBasic, wateringCanBasic, "dye");
		addColorRecipe(wateringCanHardened, wateringCanHardened, "dye");
		addColorRecipe(wateringCanReinforced, wateringCanReinforced, "dye");
		addColorRecipe(wateringCanSignalum, wateringCanSignalum, "dye");
		addColorRecipe(wateringCanResonant, wateringCanResonant, "dye");

		addColorRemoveRecipe(wateringCanBasic, wateringCanBasic);
		addColorRemoveRecipe(wateringCanHardened, wateringCanHardened);
		addColorRemoveRecipe(wateringCanReinforced, wateringCanReinforced);
		addColorRemoveRecipe(wateringCanSignalum, wateringCanSignalum);
		addColorRemoveRecipe(wateringCanResonant, wateringCanResonant);
		return true;
	}

	private static void config() {

		String category = "Item.WateringCan";
		String comment;

		enable = ThermalCultivation.CONFIG.get(category, "Enable", true);

		comment = "If TRUE, Fake Players (such as Autonomous Activators) will be able to use the Watering Can.";
		allowFakePlayers = ThermalCultivation.CONFIG.getConfiguration().getBoolean("AllowFakePlayers", category, allowFakePlayers, comment);

		comment = "If TRUE, Water Source blocks will be removed when collecting water from the world.";
		removeSourceBlocks = ThermalCultivation.CONFIG.getConfiguration().getBoolean("RemoveWaterBlocks", category, removeSourceBlocks, comment);

		int capacity = CAPACITY_BASE;
		comment = "Adjust this value to change the amount of Water (in mB) stored by a Basic Watering Can. This base value will scale with item level.";
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

		TypeEntry(String name, int capacity, int chance, int radius) {

			this.name = name;
			this.capacity = capacity;
			this.chance = chance;
			this.radius = radius;
		}
	}

	private void addEntry(int metadata, String name, int capacity, int chance, int radius) {

		typeMap.put(metadata, new TypeEntry(name, capacity, chance, radius));
	}

	private ItemStack addEntryItem(int metadata, String name, int capacity, int chance, int radius, EnumRarity rarity) {

		addEntry(metadata, name, capacity, chance, radius);
		return addItem(metadata, name, rarity);
	}

	private static Int2ObjectOpenHashMap<TypeEntry> typeMap = new Int2ObjectOpenHashMap<>();

	public static final int CAPACITY_BASE = 4000;

	public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };
	public static final int[] CHANCE = { 40, 50, 60, 70, 80 };
	public static final int[] WATER_PER_USE = { 50, 150, 300, 500, 750 };
	public static final int CHANCE_CREATIVE = 200;

	public static boolean enable = true;
	public static boolean allowFakePlayers = false;
	public static boolean removeSourceBlocks = true;

	/* REFERENCES */
	public static ItemStack wateringCanBasic;
	public static ItemStack wateringCanHardened;
	public static ItemStack wateringCanReinforced;
	public static ItemStack wateringCanSignalum;
	public static ItemStack wateringCanResonant;

	public static ItemStack wateringCanCreative;

}
