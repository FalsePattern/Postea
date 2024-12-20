package com.gtnewhorizons.postea.mixins.early;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.postea.mixins.interfaces.IChunkMixin;
import com.gtnewhorizons.postea.utility.ChunkFixerUtility;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(value = AnvilChunkLoader.class, priority = 1100)
public abstract class MixinAnvilChunkLoader {

    // This hook is compatible with Vanilla/NEID/EIDs
    @Inject(method = "readChunkFromNBT", at = @At("RETURN"), require = 1)
    private void postea$chunkHook(CallbackInfoReturnable<Chunk> cir, @Local Chunk chunk,
        @Local(argsOnly = true) World world, @Local(ordinal = 0, argsOnly = true) NBTTagCompound tag) {
        IChunkMixin chunkMixin = (IChunkMixin) chunk;
        chunkMixin.Postea$setPosteaCode(-1);
        if (tag.hasKey("POSTEA")) {
            chunkMixin.Postea$setPosteaCode(tag.getLong("POSTEA"));
        }
        // This checks if the chunk has been run with the current POSTEA_UPDATE_CODE and skips it if so.
        if (chunkMixin.Postea$getPosteaCode() == ChunkFixerUtility.POSTEA_UPDATE_CODE) return;
        for (ExtendedBlockStorage ebs : chunk.getBlockStorageArray()) {
            if (ebs == null) {
                continue;
            }
            ChunkFixerUtility.transformNormalBlocks(chunk, ebs, world);
        }
    }

    @Inject(
        method = "loadEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/NBTTagCompound;getTagList(Ljava/lang/String;I)Lnet/minecraft/nbt/NBTTagList;",
            ordinal = 1),
        require = 1)
    private void postea$transformTileEntities(CallbackInfo ci, @Local(ordinal = 0) NBTTagCompound tag,
        @Local World world, @Local Chunk chunk) {
        IChunkMixin chunkMixin = (IChunkMixin) chunk;
        // This checks if the chunk has been run with the current POSTEA_UPDATE_CODE and skips it if so.
        if (chunkMixin.Postea$getPosteaCode() == ChunkFixerUtility.POSTEA_UPDATE_CODE) return;

        ChunkFixerUtility.transformTileEntities(tag, chunk, world);
    }

    @Inject(method = "loadChunk", at = @At("RETURN"))
    private void postea$updatePosteaChunkCode(World world, int i, int j, CallbackInfoReturnable<Chunk> cir) {
        Chunk chunk = cir.getReturnValue();
        if (chunk instanceof IChunkMixin iChunkMixin) {
            if (iChunkMixin.Postea$getPosteaCode() != ChunkFixerUtility.POSTEA_UPDATE_CODE) {
                iChunkMixin.Postea$setPosteaCode(ChunkFixerUtility.POSTEA_UPDATE_CODE);
                chunk.setChunkModified();
            }
        }
    }

    @Inject(method = "writeChunkToNBT", at = @At("HEAD"))
    private void postea$writePosteaChunkCode(Chunk chunk, World world, NBTTagCompound nbtTagCompound, CallbackInfo ci) {
        if (chunk instanceof IChunkMixin iChunkMixin) {
            nbtTagCompound.setLong("POSTEA", iChunkMixin.Postea$getPosteaCode());
        }
    }

}
