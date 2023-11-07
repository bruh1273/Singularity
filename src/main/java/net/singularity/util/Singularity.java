package net.singularity.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.minecraft.screen.slot.SlotActionType.PICKUP;

public class Singularity {

//    public Singularity() {
//        super("Singularity", "All your item storage needs, in a single, infinitley dense block.");
//    }
    private boolean isUpdated = false;
    private static final MinecraftClient client = MinecraftClient.getInstance();
    DoubleSetting plrID = this.config.add(DoubleSetting.builder(
            "Storage ID",
            """
                    YOU MUST READ THIS TO UNDERSTAND HOW TO USE THIS
                    You will manually set this ID when you use the system. No two users should use the same ID.
                    Your ID is going to be what tells YOU, and the other players if your storage data is correctly up to date.
                    If your ID doesn't have a cobblestone in it's slot, that tells the client that it has to search through all the chests to update the data.
                    If your ID DOES have a cobblestone in it, then that means that you are fully up to date with the storage data.
                    If somehow, you and another user are using the same data ID, you may not be getting the latest data that has been altered.
                    This was planned for only about 25 users to be using this at once.
            """,
            25,
            0,
            0,
            25
    ).build());
    private int dataSlot;
    private final Item dataItem = Items.COBBLESTONE;
    private final HashMap<Integer, Vec3d> chestCartsInRange = new HashMap<>();
    private final HashMap<ItemStack, Integer> singleContents = new HashMap<>();
    private final HashMap<Integer, HashMap<ItemStack, Integer>> containerContents = new HashMap<>();
    private final List<StorageData> data = new ArrayList<>();
//    @Override
//    protected void disable() {
//        b = false;
//    }
    boolean b = false;
//    @Listener
    public void onTick(MinecraftClient event) {
        dataSlot = plrID;
        for(Entity e : client.player.networkHandler.getWorld().getEntities()) {
            if(e instanceof ChestMinecartEntity en) chestCartsInRange.put(en.getId(), en.getPos());
        }
        if(!b) searchChests();
        b = true;
    }
    private void searchChests() {
        if(shouldCheck()) {
            for(int eID : chestCartsInRange.keySet()) {
                if(client.player.isInRange(getEntityById(eID), 2)) {
                    Executors.newScheduledThreadPool(1).schedule(() -> {
                        client.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(getEntityById(eID), false, Hand.MAIN_HAND));
                        for(Slot s : client.player.currentScreenHandler.slots) singleContents.put(s.getStack(), s.id);
                        data.add(new StorageData(eID, singleContents));
                        containerContents.put(eID, singleContents);
                    }, 1000, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private boolean shouldCheck() {
        return getItemStackInSlot(dataSlot).isEmpty();
    }

    private boolean dataIncorrect() {
        boolean isCobble = getItemStackInSlot(dataSlot).isOf(dataItem);
        return !shouldCheck() && !isCobble;
    }

    private ItemStack getItemStackInSlot(int slotID) {
        return client.player.currentScreenHandler.slots.get(slotID).getStack();
    }

    private void updateOtherData() {
        // check if we move shizz if tru then we upd8 others
        moveToSlot(26);
    }
    private void updateOurData() {
        if(dataIncorrect()) {
            client.player.networkHandler.sendPacket(clickSlot(26, PICKUP, ItemStack.EMPTY));
            client.player.networkHandler.sendPacket(clickSlot(dataSlot, PICKUP, ItemStack.EMPTY));
            client.player.networkHandler.sendPacket(clickSlot(26, PICKUP, ItemStack.EMPTY));
        }
    }
    private void moveToSlot(int destinaton) {
        ScreenHandler handler = client.player.currentScreenHandler;
        Slot destSlot = handler.getSlot(destinaton);
        for (Slot s : handler.slots) {
            if (s.id >= ((GenericContainerScreenHandler) handler).getRows() * 9) continue;
            if (s.id == destinaton || s.id == dataSlot) continue;
            if (s.getStack().isEmpty()) continue;
            if (handler.getCursorStack().isEmpty() || handler.getCursorStack().isOf(dataItem)) client.player.networkHandler.sendPacket(clickSlot(s.id, PICKUP, s.getStack()));
            boolean notFull = destSlot.getStack().getCount() != destSlot.getStack().getMaxCount();
            if (notFull) client.player.networkHandler.sendPacket(clickSlot(destinaton, PICKUP, ItemStack.EMPTY));
        }
    }
    private Packet<?> clickSlot(int slotID, SlotActionType type, ItemStack stack) {
        return new ClickSlotC2SPacket(
                client.player.currentScreenHandler.syncId,
                client.player.currentScreenHandler.getRevision(),
                slotID,
                0,
                type,
                stack,
                new Int2ObjectArrayMap<>()
        );
    }
    private Entity getEntityById(int id) {
        for(Entity e : client.player.networkHandler.getWorld().getEntities()) {
            if(e.getId() == id) return e;
        } return null; //no such entity exists
    }
    private static final class StorageData {
        private int entityID;
        private HashMap<ItemStack, Integer> stacksAndSlotIDS;
        public StorageData(int entityID, HashMap<ItemStack, Integer> stacksAndSlotIDS) {
            this.entityID = entityID;
            this.stacksAndSlotIDS = stacksAndSlotIDS;
        }
    }
}