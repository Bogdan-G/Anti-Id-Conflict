package code.elix_x.coremods.antiidconflict.core;

import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class AntiIdConflictTransformer implements IClassTransformer{


	/*
	 * Methods to patch:
	 * BiomeGenBase: 
	 * clinit
	 * init
	 * 
	 * GETSTATIC net/minecraft/init/Blocks.grass : Lnet/minecraft/block/BlockGrass;
	 * 
	 */
	
	public static final Logger logger = LogManager.getLogger("AIC Core");

	public static boolean finshedPatching = false;
	public static boolean finshedPatchingBiomeGenBase = false;
	public static boolean finshedPatchingBiomeDictionary = false;

	@Override
	public byte[] transform(String className, String transformedName, byte[] bytes) {
		if(className.equals(AntiIdConflictTranslator.getMapedClassName("world.biome.BiomeGenBase"))){
			logger.info("##################################################");
			logger.info("Patching BiomeGenBase");
			byte[] b = patchBiomeGenBase(className, bytes);
			logger.info("Patching BiomeGenBase Completed");
			logger.info("##################################################");
			return b;
		}
		if(className.equals(AntiIdConflictTranslator.getMapedClassName("potion.Potion"))){
			logger.info("##################################################");
			logger.info("Patching Potion");
			byte[] b = patchPotion(className, bytes);
			logger.info("Patching Potion Completed");
			logger.info("##################################################");
			return b;
		}
		return bytes;
	}

	private byte[] patchPotion(String className, byte[] bytes) {
		String init = "<init>";

		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		Iterator<MethodNode> methods = classNode.methods.iterator();

		while(methods.hasNext()){
			MethodNode method = methods.next();
			if(method.name.equals(init)){
				try{
					logger.info("**************************************************");
					logger.info("Patching <init>");

					AbstractInsnNode targetNode = null;
					
					for(AbstractInsnNode currentNode : method.instructions.toArray()){
						if(currentNode.getOpcode() == Opcodes.ILOAD){
							targetNode = currentNode;
							break;
						}
					}
					
					method.instructions.insert(targetNode, new MethodInsnNode(Opcodes.INVOKESTATIC, "code.elix_x.coremods.antiidconflict.core.AsmHooks".replace(".", "/"), "getPotionID", "(I)I", false));
					
					logger.info("Patching <init> completed");
					logger.info("**************************************************");
				} catch(Exception e){
					logger.error("Patching <init> failed with exception: ", e);
					logger.info("**************************************************");
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchBiomeGenBase(String className, byte[] bytes) {
		String clinit = "<clinit>";
		String init = "<init>";

		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		Iterator<MethodNode> methods = classNode.methods.iterator();

		while(methods.hasNext()){
			MethodNode method = methods.next();
			if(method.name.equals(clinit)){
				try{
					logger.info("**************************************************");
					logger.info("Patching <clinit>");

					AbstractInsnNode currentNode = null;
					AbstractInsnNode targetNode = null;
					int place = -1;
					int index = -1;

					Iterator<AbstractInsnNode> iter = method.instructions.iterator();

					while (iter.hasNext())
					{
						index++;
						currentNode = iter.next();

						if(currentNode.getOpcode() == Opcodes.SIPUSH){
							IntInsnNode in = (IntInsnNode) currentNode;
							if(in.operand == 256){
								targetNode = currentNode;
								place = index;
								break;
							}
						}
					}

					//				m.instructions.insert(targetNode, new InsnNode(Opcodes.ICONST_0));
					//				m.instructions.insert(targetNode, new IntInsnNode(Opcodes.SIPUSH, biomesLimit));
					//INVOKESPECIAL code/elix_x/coremods/antiidconflict/AntiIdConflictBase.getLimitation ()I
					method.instructions.insert(targetNode, new MethodInsnNode(Opcodes.INVOKESTATIC, "code/elix_x/coremods/antiidconflict/core/AsmHooks", "getLimitation", "()I"));
					method.instructions.remove(targetNode);

					logger.info("Patching <clinit> completed");
					logger.info("**************************************************");
				} catch(Exception e){
					logger.error("Patching <clinit> failed with exception: ", e);
				}
			}
			if(method.name.equals(init) && method.desc.equals("(IZ)V")){
				try{
					logger.info("**************************************************");
					logger.info("Patching <init>");

					AbstractInsnNode currentNode = null;
					AbstractInsnNode targetNode = null;
					int place = -1;
					int index = -1;

					Iterator<AbstractInsnNode> iter = method.instructions.iterator();
//					System.out.println(AntiIdConflictTranslator.getMapedClassName("world.biome.BiomeGenBase").replace(".", "/"));
					while (iter.hasNext())
					{
						index++;
						currentNode = iter.next();

						if(currentNode.getOpcode() == Opcodes.PUTFIELD){
							FieldInsnNode field = (FieldInsnNode) currentNode;
//							System.out.println(field.owner);
							if(field.owner.equals(AntiIdConflictTranslator.getMapedClassName("world.biome.BiomeGenBase").replace(".", "/")) && field.name.equals(AntiIdConflictTranslator.getMapedFieldName("BiomeGenBase", "field_76756_M", "biomeID"))){
								targetNode = currentNode;
								place = index;
								break;
							}
						}
					}

					/*
					 * ALOAD 0
					 * ILOAD 1
					 * PUTFIELD net/minecraft/world/biome/BiomeGenBase.biomeID : I
					 * 
					 *  ALOAD 0
					 *  ILOAD 1
					 *  ILOAD 2
					 *  INVOKESTATIC code/elix_x/coremods/antiidconflict/AntiIdConflictBase.getBiomeID (IZ)I
					 *  PUTFIELD code/elix_x/coremods/antiidconflict/AntiIdConflictBase.biomeID : I 
					 */
					method.instructions.insertBefore(targetNode, createNewListAndFillWith(new VarInsnNode(Opcodes.ILOAD, 2), new MethodInsnNode(Opcodes.INVOKESTATIC, "code/elix_x/coremods/antiidconflict/core/AsmHooks", "getBiomeID", "(IZ)I")));

					logger.info("Patching <init> completed");
					logger.info("**************************************************");
				} catch(Exception e){
					logger.error("Patching <init> failed with exception: ", e);
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private InsnList createNewListAndFillWith(Object... nodes) {
		InsnList list = new InsnList();
		for(Object node : nodes){
			if(node instanceof AbstractInsnNode){
				list.add((AbstractInsnNode) node);
			}
			if(node instanceof InsnList){
				list.add(list);
			}
		}
		return list;
	}
}
