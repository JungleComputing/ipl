import java.io.*;

class VirtualScreen implements java.io.Serializable
{
	int w,h;
	int[][] bitmap;
	int pixel;
    
    
	VirtualScreen(GfxColor c)
	{
		pixel = c.GetRGB();
	}


	VirtualScreen(int w, int h)
	{
		this.w = w;
		this.h = h;

		bitmap = new int[w][h];
	}


	void Dump(String filename)
	{
		FileOutputStream f = null;
		BufferedOutputStream bf = null;

		try {
			f = new FileOutputStream(filename);
			bf  = new BufferedOutputStream(f);
		} catch (Exception e) {
			System.out.println("Could not open output file");
			System.exit(99);
		}

		PrintWriter p = new PrintWriter(bf);

		p.println("P3");
		p.println(w + " " + h);
		p.println("255");
      
		for (int py = 0; py<h; py++) {
			for (int px = 0; px<w; px++) {
				int c = bitmap[px][py];
		      
				int r = GfxColor.getR(c);
				int g = GfxColor.getG(c);
				int b = GfxColor.getB(c);
				p.print(r + " " + g + " " + b + " "); 
			}
			p.println();
		}

		try {
			p.flush();
			bf.flush();
			f.flush();
			bf.close();
		} catch (Exception e) {
			System.err.println("Exception on close: " + e);
		}
	}
	

	void Set(int x, int y, int w, int h, VirtualScreen s)
	{
		int sx, sy;
	
		if (s.bitmap == null)
			{
				 
				bitmap[x][y] = s.pixel;
				return;
			}

		sx = 0;
		for (int px = x; px<x+w; px++) {
			System.arraycopy(s.bitmap[sx], 0, bitmap[px], y, h);
			sx++;
		}
	}
}
