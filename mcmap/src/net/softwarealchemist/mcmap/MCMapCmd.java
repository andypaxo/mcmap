package net.softwarealchemist.mcmap;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.imageio.ImageIO;

public class MCMapCmd {

    class Vec2
    {
        public int x;
        public int y;

        public Vec2(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        public boolean Equals(Vec2 other)
        {
            return x == other.x && y == other.y;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj != null && obj.getClass() == Vec2.class && Equals((Vec2) obj);
        }

        @Override
        public int hashCode()
        {
            return (x*397) ^ y;
        }
    }

	HashMap<Integer, Integer> colors = new HashMap<Integer, Integer>();
	
	int minX, minZ, maxX, maxZ;
	
	public static void main(String[] args) {
		String homeDir = System.getProperty("user.home");
		String mcDir = homeDir + "\\AppData\\Roaming\\.minecraft\\saves\\";
		String worldDir = mcDir + "mess_with_2\\region\\";
		
		new MCMapCmd().run(args.length > 0 ? args[0] + "/region/" : worldDir, args.length > 1 ? args[1] + "/" : "./");
	}
	
	private void run(String worldDir, String outDir) {
        createColors();
		readAllFiles(worldDir, outDir);
		writePage(outDir);
	}

	private void createColors() {
		colors.put(BlockType.STONE, Colors.Gray);
        colors.put(BlockType.STONE_BRICK, Colors.DarkGray);
        colors.put(BlockType.DIRT, Colors.Brown);
        colors.put(BlockType.GRASS, Colors.Green);
        colors.put(BlockType.TALL_GRASS, Colors.DarkGreen);
        colors.put(BlockType.STATIONARY_WATER, Colors.DodgerBlue);
        colors.put(BlockType.WATER, Colors.DodgerBlue);
        colors.put(BlockType.COAL_ORE, Colors.DimGray);
        colors.put(BlockType.GRAVEL, Colors.LightGray);
        colors.put(BlockType.SAND, Colors.BlanchedAlmond);
        colors.put(BlockType.SANDSTONE, Colors.BurlyWood);
        colors.put(BlockType.SANDSTONE_STAIRS, Colors.DarkKhaki);
        colors.put(BlockType.WOOD, Colors.Peru);
        colors.put(BlockType.WOOD_PLANK, Colors.Bisque);
        colors.put(BlockType.WOOD_SLAB, Colors.Bisque);
        colors.put(BlockType.DOUBLE_WOOD_SLAB, Colors.Bisque);
        colors.put(BlockType.LEAVES, Colors.Leaves);
        colors.put(BlockType.IRON_ORE, Colors.DarkOrange);
        colors.put(BlockType.CROPS, Colors.GreenYellow);
        colors.put(BlockType.FARMLAND, Colors.Firebrick);
        colors.put(BlockType.LILLY_PAD, Colors.Green);
        colors.put(BlockType.LAVA, Colors.Red);
        colors.put(BlockType.STATIONARY_LAVA, Colors.Red);
	}
	
	private void readAllFiles(String worldDir, String outDir) {
		File dir = new File(worldDir);
		File[] files = dir.listFiles();
		for (File file : files)
			output(file, outDir);
	}

	private void output(File regionFile, String outDir) {
		RegionFile region = new RegionFile(regionFile);

		String filename = regionFile.getName();
		String[] nameParts = filename.split("\\.");
		int regionX = Integer.parseInt(nameParts[1]);
		int regionZ = Integer.parseInt(nameParts[2]);
		
        minX = Math.min(minX, regionX);
        minZ = Math.min(minZ, regionZ);
        maxX = Math.max(maxX, regionX);
        maxZ = Math.max(maxZ, regionZ);

		BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
		
		for (int chunkX = 0; chunkX < 32; chunkX++)
			for (int chunkZ = 0; chunkZ < 32; chunkZ++)
				writeChunkToImage(region, image, chunkX, chunkZ);

		writeImageToPng(outDir + filename, image);
		System.out.println();
	}

	private void writeChunkToImage(RegionFile region, BufferedImage image,
			int chunkX, int chunkZ) {
		if (chunkZ == 0)
			System.out.print(".");
		
		DataInputStream chunkDataInputStream = region.getChunkDataInputStream(chunkX, chunkZ);
		if (chunkDataInputStream == null)
			return;
		
		try {
			Tag tag = Tag.readFrom(chunkDataInputStream);
			Tag[] sections = (Tag[]) tag.findTagByName("Sections").getValue();	
	
			byte[][] sectionData = new byte[sections.length][];
			for (int i = 0; i < sections.length; i++)
				sectionData[i] = (byte[]) sections[i].findTagByName("Blocks").getValue();
			int maxY = sections.length * 16 - 1;
			
			int blockID;
			byte[] section;
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = maxY; y >= 0; y--) {
						section = sectionData[y / 16];
						blockID = (int)section[(y % 16) * 256 + z * 16 + x];
						if (colors.containsKey(blockID)) {
							image.setRGB(chunkX * 16 + x, chunkZ * 16 + z, colors.get(blockID));
							break;
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeImageToPng(String filename, BufferedImage image) {
		try {
			ImageIO.write(image, "png", new File(filename + ".png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private void writePage(String outDir) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outDir + "index.html");
			writer.append("<!DOCTYPE html>\n");
			writer.append("<html><head><title>Minecraft!</title></head>\n");
			writer.append("<title>Minecraft!</title>\n");
			writer.append("<style type='text/css'>* { margin:0; padding: 0; } table { font-size: 0; border-collapse: collapse }</style>\n");
			writer.append("</head>\n");
			writer.append("<body><table>");
			for (int z = minZ; z <= maxZ; z++) {
				writer.append("<tr>");	
				for (int x = minX; x <= maxX; x++) {
					writer.append("<td><img src='r."+x+"."+z+".mca.png'/></td>");
				}
				writer.append("</tr>");
			}
			writer.append("\n</body></table>");
			writer.flush();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
}
