//
// Class Matrix
//
// recursive part of quad tree
//
final class Matrix extends Leaf implements java.io.Serializable {
	Matrix _00, _01, _10, _11;

	public Matrix(int task, int rec, int loop, float dbl, boolean flipped) {
		super(task, rec, loop, dbl, flipped); // construct Leaf

		if(task + rec <= 0) return;

		// Quad tree is recursive for both the task and the serial
		// recursion levels 

		if (task > 0) {
			task--;
		} else {
			rec--;
		}

		_00 = new Matrix(task, rec, loop, dbl, flipped);
		_01 = new Matrix(task, rec, loop, (flipped ? -dbl : dbl), flipped);
		_10 = new Matrix(task, rec, loop, dbl, flipped);
		_11 = new Matrix(task, rec, loop, (flipped ? -dbl : dbl), flipped);
	}


	public float sum(int task, int rec) {
		float s = 0.0f;
		if (task + rec > 0) {
			if (task > 0) {
				task--;
			} else {
				rec--;
			}
		  
			s += _00.sum(task, rec);
			s += _01.sum(task, rec);
			s += _10.sum(task, rec);
			s += _11.sum(task, rec);
		} else {
			// task + rec == 0 here
			s = super.sum();
		}
		return s;
	}


	public void print(int task, int rec) {
		if (task + rec > 0) {
			if (task > 0) {
				task--;
			} else {
				rec--;
			}

			_00.print(task, rec);
			_01.print(task, rec);
			_10.print(task, rec);
			_11.print(task, rec);
		} else {
			super.print();
		}
	}


	public boolean check(int task, int rec, float result) {
		boolean ok = true;

		if (task + rec > 0) {
			if (task > 0) {
				task--;
			} else {
				rec--;
			}

			ok &= _00.check(task, rec, result);
			ok &= _01.check(task, rec, result);
			ok &= _10.check(task, rec, result);
			ok &= _11.check(task, rec, result);
		} else {
			ok &= super.check(result);
		}

		return ok;
	}


	public void recMatMul(int depth, Matrix a, Matrix b) {
		if (depth == 0) {
			// pass Matrices as local variables 
			// loopMatMul(a, b);
			multiplyStride2(a, b);
		} else {
			// depth-1 stuff should be faster than creating a new Size thing
			// each time
			_00.recMatMul(depth-1, a._00, b._00);
			_01.recMatMul(depth-1, a._00, b._01);
			_10.recMatMul(depth-1, a._10, b._00);
			_11.recMatMul(depth-1, a._10, b._01);

			_00.recMatMul(depth-1, a._01, b._10);
			_01.recMatMul(depth-1, a._01, b._11);
			_10.recMatMul(depth-1, a._11, b._10);
			_11.recMatMul(depth-1, a._11, b._11);
		}
	}
}
