
public class Label{
	public int la;
	public int lb;
	public int lc;
	public int size;
	
	public Label(int la, int lb){
		this.la = la;
		this.lb = lb;
		this.lc = 0;
		this.size = 2;
	}
	
	public Label(int la, int lb, int lc){
		this.la = la;
		this.lb = lb;
		this.lc = lc;
		this.size = 3;
	}
	
	public boolean isForLabels(){
		return this.size == 3;
	}
	
	public boolean isIfLabels(){
		return this.size == 2;
	}
	
}