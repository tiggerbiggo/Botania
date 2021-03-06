/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 *
 * File Created @ [Apr 13, 2014, 7:14:54 PM (GMT)]
 */
package vazkii.botania.common.item.equipment.tool.manasteel;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.item.ISortableTool;
import vazkii.botania.api.mana.IManaUsingItem;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.common.item.equipment.tool.ToolCommons;
import vazkii.botania.common.lib.LibObfuscation;

import javax.annotation.Nonnull;
import java.util.Map;

public class ItemManasteelShovel extends ShovelItem implements IManaUsingItem, ISortableTool {

	private static final Map<Block, BlockState> HOE_LOOKUP = ObfuscationReflectionHelper.getPrivateValue(HoeItem.class, null, LibObfuscation.HOE_LOOKUP);
	private static final int MANA_PER_DAMAGE = 60;

	public ItemManasteelShovel(Properties props) {
		this(BotaniaAPI.MANASTEEL_ITEM_TIER, props);
	}

	public ItemManasteelShovel(IItemTier mat, Properties props) {
		super(mat, 1.5F, -3.0F, props);
	}

	@Override
	public boolean hitEntity(ItemStack par1ItemStack, LivingEntity par2EntityLivingBase, @Nonnull LivingEntity par3EntityLivingBase) {
		ToolCommons.damageItem(par1ItemStack, 1, par3EntityLivingBase, MANA_PER_DAMAGE);
		return true;
	}

	@Override
	public boolean onBlockDestroyed(@Nonnull ItemStack stack, @Nonnull World world, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nonnull LivingEntity entity) {
		if (state.getBlockHardness(world, pos) != 0F)
			ToolCommons.damageItem(stack, 1, entity, MANA_PER_DAMAGE);

		return true;
	}

	@Nonnull
	@Override
	public ActionResultType onItemUse(ItemUseContext ctx) {
		ItemStack stack = ctx.getItem();
		PlayerEntity player = ctx.getPlayer();
		World world = ctx.getWorld();
		BlockPos pos = ctx.getPos();

		if (player == null || !player.canPlayerEdit(pos, ctx.getFace(), stack))
			return ActionResultType.PASS;

		Block block = world.getBlockState(pos).getBlock();
		BlockState converted = field_195955_e.get(block);

		if (converted == null) {
			UseHoeEvent event = new UseHoeEvent(ctx);

			if (MinecraftForge.EVENT_BUS.post(event))
				return ActionResultType.FAIL;

			if (event.getResult() == Event.Result.ALLOW) {
				ToolCommons.damageItem(stack, 1, player, MANA_PER_DAMAGE);
				return ActionResultType.SUCCESS;
			}
			converted = HOE_LOOKUP.get(block);
		}
		if (converted == null)
			return ActionResultType.PASS;

		if (ctx.getFace() != Direction.DOWN && world.getBlockState(pos.up()).getBlock().isAir(world.getBlockState(pos.up()), world, pos.up())) {
			world.playSound(null, pos, converted.getSoundType().getStepSound(),
					SoundCategory.BLOCKS,
					(converted.getSoundType().getVolume() + 1.0F) / 2.0F,
					converted.getSoundType().getPitch() * 0.8F);

			if (world.isRemote)
				return ActionResultType.SUCCESS;
			else {
				world.setBlockState(pos, converted);
				ToolCommons.damageItem(stack, 1, player, MANA_PER_DAMAGE);
				return ActionResultType.SUCCESS;
			}
		}

		return ActionResultType.PASS;
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity player, int par4, boolean par5) {
		if (!world.isRemote && player instanceof PlayerEntity && stack.getDamage() > 0 && ManaItemHandler.requestManaExactForTool(stack, (PlayerEntity) player, MANA_PER_DAMAGE * 2, true))
			stack.setDamage(stack.getDamage() - 1);
	}

	@Override
	public boolean usesMana(ItemStack stack) {
		return true;
	}

	@Override
	public ToolType getSortingType(ItemStack stack) {
		return ToolType.SHOVEL;
	}

	@Override
	public int getSortingPriority(ItemStack stack) {
		return ToolCommons.getToolPriority(stack);
	}
}
