import java.io.*;

class VirtualScreen implements java.io.Serializable
{
	int w,h;
	int[][] bitmap;
	int pixel;
    
    
	VirtualScreen(GfxColor c)
	{
		pixel = c.GetRGB();
		w = h = 1;
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
	
	boolean check(String filename) {
	    try {
		FileInputStream s = new FileInputStream(filename);
		StreamTokenizer d = new StreamTokenizer(new InputStreamReader(s));

		d.eolIsSignificant(false);

		d.nextToken();
		d.nextToken();
		if (d.nval != (double) w) return false;
		d.nextToken();
		if (d.nval != (double) h) return false;
		d.nextToken();
		if (d.nval != 255.0) return false;
		for (int py = 0; py<h; py++) {
			for (int px = 0; px<w; px++) {
				int c = bitmap[px][py];
				int r = GfxColor.getR(c);
				int g = GfxColor.getG(c);
				int b = GfxColor.getB(c);
				d.nextToken();
				if (d.nval != (double) r) return false;
				d.nextToken();
				if (d.nval != (double) g) return false;
				d.nextToken();
				if (d.nval != (double) b) return false;
			}
		}

		return true;

	    } catch (Exception e) {
		System.out.println("check error: " + e);
		return false;
	    }
	}

	void Set(int x, int y, int w, int h, VirtualScreen s)
	{
		int sx, sy;
		
		if (w != s.w || h != s.h) {
			System.out.println("RAY:AAARGG: w = " + w + ", h = " + h + ", s.w = " + s.w + ", s.h = " + s.h);

			if(s.bitmap != null) {
				System.out.println("s.bitmaplen = " + s.bitmap.length);
				if(s.bitmap.length > 0) {
					System.out.println("s.bitmap.w = " + s.bitmap[0].length);
				}
			}
		}

		if (s.bitmap == null) {
			if(w != 1 || h != 1) {
				System.out.println("RAY: EEEEEK");
			}

			bitmap[x][y] = s.pixel;
			if(s.pixel == 0) {
				System.out.println("RAY 0 pixel!");
			}
			return;
		}

		if(w == 1 || h == 1) {
			System.out.println("RAY: EEEEEK2");
		}

		sx = 0;
		for (int px = x; px<x+w; px++) {
			System.arraycopy(s.bitmap[sx], 0, bitmap[px], y, h);

			int sum = 0;
			for(int i=0; i<h; i++) {  
				sum += bitmap[px][y+i];
			}
			
			if(sum == 0) {
				System.out.println("RAY: 0 array!");
			}

			sx++;
		}
	}
}
