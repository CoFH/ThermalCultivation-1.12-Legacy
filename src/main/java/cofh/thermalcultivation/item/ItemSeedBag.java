package cofh.thermalcultivation.item;

import cofh.api.item.IInventoryContainerItem;
import cofh.api.item.IMultiModeItem;
import cofh.api.item.INBTCopyIngredient;
import cofh.core.gui.container.InventoryContainerItemWrapper;
import cofh.core.init.CoreEnchantments;
import cofh.core.init.CoreProps;
import cofh.core.item.IEnchantableItem;
import cofh.core.item.ItemMulti;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.CoreUtils;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalcultivation.ThermalCultivation;
import cofh.thermalcultivation.gui.GuiHandler;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;

public class ItemSeedBag extends ItemMulti implements IInitializer, IMultiModeItem, IInventoryContainerItem, IEnchantableItem, INBTCopyIngredient {

	public ItemSeedBag() {

		super("thermalcultivation");

		setUnlocalizedName("seed_bag");
		setCreativeTab(ThermalCultivation.tabCommon);

		setHasSubtypes(true);
		setMaxStackSize(1);
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {

		if (isInCreativeTab(tab)) {
			for (int metadata : itemList) {
				items.add(setDefaultInventoryTag(new ItemStack(this, 1, metadata)));
			}
		}
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flagIn) {

		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		int range = getRange(stack) * 2 + 1;

		tooltip.add(StringHelper.getInfoText("info.thermalcultivation.seed_bag.0"));
		tooltip.add(StringHelper.localize("info.cofh.area") + ": " + range + "x" + range);

		if (getNumModes(stack) > 1) {
			tooltip.add(StringHelper.localizeFormat("info.thermalcultivation.seed_bag.1", StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));
		}
		ItemHelper.addInventoryInformation(stack, tooltip);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {

		ItemStack stack = player.getHeldItem(hand);
		if (CoreUtils.isFakePlayer(player) || hand != EnumHand.MAIN_HAND) {
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}
		if (!stack.hasTagCompound()) {
			setDefaultInventoryTag(stack);
		}
		if (player.isSneaking()) {
			player.openGui(ThermalCultivation.instance, GuiHandler.SEED_BAG_ID, world, 0, 0, 0);
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		ItemStack stack = player.getHeldItem(hand);
		if (CoreUtils.isFakePlayer(player) || hand != EnumHand.MAIN_HAND) {
			return EnumActionResult.FAIL;
		}
		if (!stack.hasTagCompound()) {
			setDefaultInventoryTag(stack);
		}
		if (facing != EnumFacing.UP) {
			return EnumActionResult.FAIL;
		}
		if (ServerHelper.isServerWorld(world)) {
			int range = getRange(stack);
			InventoryContainerItemWrapper wrapper = new InventoryContainerItemWrapper(stack);
			for (int x = -range; x <= range; x++) {
				for (int z = -range; z <= range; z++) {
					BlockPos target = pos.add(x, 0, z);

					for (int i = 0; i < wrapper.getSizeInventory(); i++) {
						ItemStack item = wrapper.getStackInSlot(i);
						if (item.isEmpty()) {
							continue;
						}
						if (!(item.getItem() instanceof IPlantable)) {
							continue;
						}
						IPlantable plantable = (IPlantable) item.getItem();
						IBlockState state = world.getBlockState(target);
						if (player.canPlayerEdit(target.offset(facing), facing, item) && state.getBlock().canSustainPlant(state, world, target, EnumFacing.UP, plantable) && world.isAirBlock(target.up())) {
							world.setBlockState(target.up(), plantable.getPlant(world, target));
							if (player instanceof EntityPlayerMP) {
								CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, target.up(), item);
							}
							if (!isCreative(stack)) {
								item.shrink(1);
							}
							break;
						}
					}
				}
			}
			wrapper.markDirty();
		}

		return EnumActionResult.SUCCESS;
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
	public int getItemEnchantability() {

		return 10;
	}

	/* IEnchantableItem */
	@Override
	public boolean canEnchant(ItemStack stack, Enchantment enchantment) {

		return ItemHelper.getItemDamage(stack) != CREATIVE && enchantment == CoreEnchantments.holding;
	}

	/* IItemColor */
	public static int colorMultiplier(ItemStack stack, int tintIndex) {

		if (tintIndex != 1) {
			return 0xFFFFFF;
		}
		Item seedItem = getCurrentSeed(stack);
		if (seedItem == null) {
			return 0xFFFFFF;
		}
		if (colorMap.containsKey(seedItem)) {
			return colorMap.get(seedItem);
		} else {
			int color = getSeedColor(seedItem);
			colorMap.put(seedItem, color);
			return color;
		}
	}

	/* HELPERS */
	public ItemStack setDefaultInventoryTag(ItemStack container) {

		if (container.getTagCompound() == null) {
			container.setTagCompound(new NBTTagCompound());
		}
		container.getTagCompound().setInteger("Mode", getNumModes(container) - 1);
		return container;
	}

	@SideOnly (Side.CLIENT)
	public static int getSeedColor(Item seed) {

		TextureAtlasSprite sprite = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(new ItemStack(seed)).getParticleTexture();
		int[] data = sprite.getFrameTextureData(0)[0];
		long[] avgColor = new long[3];
		int red;
		int green;
		int blue;
		int dropped = 0;
		int minDiversion = 15;

		for (int j = 0; j < sprite.getIconHeight(); j++) {
			for (int i = 0; i < sprite.getIconWidth(); i++) {
				int color = data[j * sprite.getIconWidth() + i];
				red = (color) & 0xFF;
				green = (color >> 8) & 0xFF;
				blue = (color >> 16) & 0xFF;
				if (Math.abs(red - green) > minDiversion || Math.abs(red - blue) > minDiversion || Math.abs(green - blue) > minDiversion) {
					avgColor[2] += red;
					avgColor[1] += green;
					avgColor[0] += blue;
				} else {
					dropped++;
				}
			}
		}
		int count = sprite.getIconWidth() * sprite.getIconHeight() - dropped;
		int avgR = (int) (avgColor[2] / count);
		int avgG = (int) (avgColor[1] / count);
		int avgB = (int) (avgColor[0] / count);

		return (avgR) | (avgG << 8) | (avgB << 16);
	}

	public static boolean onItemPickup(EntityItemPickupEvent event, ItemStack stack) {

		ItemStack eventItem = event.getItem().getItem();
		int count = eventItem.getCount();

		if (getCurrentSeed(stack) == null || !(eventItem.getItem().equals(getCurrentSeed(stack)))) {
			return false;
		}
		InventoryContainerItemWrapper inv = new InventoryContainerItemWrapper(stack);
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack slot = inv.getStackInSlot(i);
			if (ItemHandlerHelper.canItemStacksStackRelaxed(eventItem, slot)) {
				int fill = slot.getMaxStackSize() - slot.getCount();
				if (fill > eventItem.getCount()) {
					slot.setCount(slot.getCount() + eventItem.getCount());
				} else {
					slot.setCount(slot.getMaxStackSize());
				}
				eventItem.splitStack(fill);
			} else if (slot.isEmpty()) {
				inv.setInventorySlotContents(i, eventItem.copy());
				eventItem.setCount(0);
			}
			if (eventItem.isEmpty()) {
				break;
			}
		}
		if (eventItem.getCount() != count) {
			stack.setAnimationsToGo(5);
			EntityPlayer player = event.getEntityPlayer();
			player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((MathHelper.RANDOM.nextFloat() - MathHelper.RANDOM.nextFloat()) * 0.7F + 1.0F) * 2.0F);
			inv.markDirty();
		}
		eventItem.setCount(0);
		return eventItem.isEmpty();
	}

	public static boolean isCreative(ItemStack stack) {

		return ItemHelper.getItemDamage(stack) == CREATIVE;
	}

	public static int getLevel(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).level;
	}

	public static Item getCurrentSeed(ItemStack stack) {

		InventoryContainerItemWrapper wrapper = new InventoryContainerItemWrapper(stack);
		for (int i = 0; i < wrapper.getSizeInventory(); i++) {
			ItemStack item = wrapper.getStackInSlot(i);
			if (!item.isEmpty()) {
				return item.getItem();
			}
		}
		return null;
	}

	public int getMaxRange(int metadata) {

		if (!typeMap.containsKey(metadata)) {
			return 0;
		}
		return typeMap.get(metadata).range;
	}

	public int getRange(ItemStack stack) {

		return 1 + getMode(stack);
	}

	public static boolean hasHoldingEnchant(ItemStack stack) {

		return EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack) > 0;
	}

	public static int getStorageIndex(ItemStack stack) {

		if (isCreative(stack) || !typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		int level = typeMap.get(ItemHelper.getItemDamage(stack)).level;
		int enchant = EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack);

		return Math.min(1 + level + enchant, CoreProps.STORAGE_SIZE.length - 1);
	}

	/* IMultiModeItem */
	@Override
	public int getNumModes(ItemStack stack) {

		return getMaxRange(ItemHelper.getItemDamage(stack));
	}

	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_TRIPWIRE_CLICK_ON, SoundCategory.PLAYERS, 0.6F, 1.0F - 0.1F * getMode(stack));

		int radius = getRange(stack) * 2 + 1;
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentString(StringHelper.localize("info.cofh.area") + ": " + radius + "x" + radius));
	}

	/* IInventoryContainerItem */
	@Override
	public int getSizeInventory(ItemStack container) {

		return CoreProps.STORAGE_SIZE[getStorageIndex(container)];
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		config();

		seedBagBasic = addEntryItem(0, "standard0", 0, 1, EnumRarity.COMMON);
		seedBagHardened = addEntryItem(1, "standard1", 1, 2, EnumRarity.COMMON);
		seedBagReinforced = addEntryItem(2, "standard2", 2, 3, EnumRarity.UNCOMMON);
		seedBagSignalum = addEntryItem(3, "standard3", 3, 4, EnumRarity.UNCOMMON);
		seedBagResonant = addEntryItem(4, "standard4", 4, 5, EnumRarity.RARE);

		seedBagCreative = addEntryItem(CREATIVE, "creative", 4, 5, EnumRarity.EPIC);

		ThermalCultivation.proxy.addIModelRegister(this);

		return true;
	}

	@Override
	public boolean register() {

		if (!enable) {
			return false;
		}

		// @formatter:off

        // @formatter:on

		return true;
	}

	private static void config() {

		String category = "Item.SeedBag";
		String comment;

		enable = ThermalCultivation.CONFIG.get(category, "Enable", true);
	}

	/* ENTRY */
	public class TypeEntry {

		public final String name;
		public final int level;
		public final int range;

		TypeEntry(String name, int level, int range) {

			this.name = name;
			this.level = level;
			this.range = range;
		}
	}

	private void addEntry(int metadata, String name, int level, int radius) {

		typeMap.put(metadata, new TypeEntry(name, level, radius));
	}

	private ItemStack addEntryItem(int metadata, String name, int level, int radius, EnumRarity rarity) {

		addEntry(metadata, name, level, radius);
		return addItem(metadata, name, rarity);
	}

	private static TIntObjectHashMap<TypeEntry> typeMap = new TIntObjectHashMap<>();
	private static HashMap<Item, Integer> colorMap = new HashMap<>();

	public static final int CREATIVE = 32000;

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack seedBagBasic;
	public static ItemStack seedBagHardened;
	public static ItemStack seedBagReinforced;
	public static ItemStack seedBagSignalum;
	public static ItemStack seedBagResonant;

	public static ItemStack seedBagCreative;

}
