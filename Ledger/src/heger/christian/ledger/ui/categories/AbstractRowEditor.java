package heger.christian.ledger.ui.categories;


public class AbstractRowEditor {
//	public interface RowEditorListener {
//		/**
//		 * Called when the passed row has been put into editing mode.
//		 * @param row - The row that was put into editing mode
//		 */
//		public void onEditStart(ViewGroup row);
//		/**
//		 * Called when the passed row has been put out of editing mode.
//		 * @param row - The row that was put out of editing mode
//		 * @param caption - The new caption 
//		 */
//		public void onEditStop(ViewGroup row, String caption);
//	}
//
//	public interface ViewTransformer {
//		public View transform(View view);	
//	}
//	
//	/**
//	 * Standard view transformer that replaces a <code>TextView</code> with an <code>EditText</code>	 
//	 * @author chris
//	 *
//	 */
//	protected class TextViewTransformer implements ViewTransformer {
//		private boolean endEditOnEnter = true;
//		
//		public View transform(View original) {	
//			final TextView textview = (TextView) original;
//			// Create a new EditText to replace caption text view
//			final EditText edittext = new EditText(original.getContext()) {
//				private boolean marginSet = false;
//				@Override
//				// Override this so we can adjust the top and bottom margin for a nice and clean UI during 
//				// the first measuring cycle. 
//				// Can't do this directly since the height information will be unavailable prior the first layout.
//				protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//					super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//					if (!marginSet) {					
//						int dh = getMeasuredHeight() - textview.getHeight();
//
//						MarginLayoutParams layoutParams = (MarginLayoutParams) getLayoutParams();
//						layoutParams.leftMargin -= getPaddingLeft();
//						layoutParams.topMargin -= dh/2; //getBaseline() - txtCaption.getBaseline();
//						//					layoutParams.rightMargin -= getPaddingRight();
//						layoutParams.bottomMargin -= dh/2; //(getMeasuredHeight() - getBaseline()) - (txtCaption.getHeight() - txtCaption.getBaseline());
//						setLayoutParams(layoutParams);
//						marginSet = true;
//					}
//				}
//			};			
//			edittext.setText(textview.getText());
//			MarginLayoutParams layoutParams = (MarginLayoutParams) textview.getLayoutParams();
//			//		oldMarginLeft = layoutParams.leftMargin; oldMarginTop = layoutParams.topMargin; oldMarginRight = layoutParams.rightMargin;
//			//		oldMarginRight = layoutParams.rightMargin;
//			edittext.setLayoutParams(layoutParams);
//			edittext.setTextSize(TypedValue.COMPLEX_UNIT_PX, textview.getTextSize());
//			edittext.setLines(textview.getLineCount());
//			edittext.setHorizontallyScrolling(false); // Wrap words into the edittext
//			if (endEditOnEnter) {
//				edittext.setImeOptions(EditorInfo.IME_ACTION_DONE);
//				// Set listener so that when "done" is selected on the IME, editing is stopped
//				edittext.setOnEditorActionListener(new OnEditorActionListener() {			
//					public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//						if (actionId == EditorInfo.IME_ACTION_DONE) {
//							stopEditing();
//							return true;
//						}
//						return false;
//					}			
//				});
//			}
//			return edittext;
//		}
//	}
//
//	protected class ImageViewTransformer implements ViewTransformer {
//		public View transform(View view) {
//			// Create "Done" button to replace edit button 
//			ImageView original = (ImageView) row.findViewById(R.id.btn_edit);
//			ImageView result = new ImageView(row.getContext());
//			result.setId(R.id.btn_done);
//			
//			result.setScaleType(original.getScaleType());
//			result.setScaleX(original.getScaleX());
//			result.setScaleY(original.getScaleY());
//			result.setImageResource(R.drawable.ic_checkmark);
//			result.setFocusable(original.isFocusable());
//			result.setBackground(original.getBackground());
//			result.setLayoutParams(original.getLayoutParams());
//			result.setPadding(original.getPaddingLeft(), original.getPaddingTop(), original.getPaddingRight(), original.getPaddingBottom());
//			result.setOnClickListener(new OnClickListener() {			
//				public void onClick(View v) {
//					stopEditing();				
//				}
//			});
//			return result;
//	}
//	}
//	private ViewGroup row;
//	private RowEditorListener listener;
//
//	private SparseArray<View> replacedViews;
//	private SparseArray<ViewTransformer> transformers = new SparseArray<ViewTransformer>();
//	
//	private int initialFocus = 0;
//	
//	public AbstractRowEditor() {
//	}
//
//	
//	public void startEditing() {
//		if (isEditing()) 
//			return;
//		
//		class ReplacementInformation {
//			View view;
//			int index;
//			public ReplacementInformation(View view, int index) {
//				this.view = view; this.index = index;
//			}
//		}
//		
//		final ViewGroup row = getRow();
//		replacedViews = new SparseArray<View>(transformers.size());
//		ArrayList<ReplacementInformation> replacements = new ArrayList<ReplacementInformation>(transformers.size());
//		for (int i = 0; i < transformers.size(); i++) {
//			View current = row.findViewById(transformers.keyAt(i));
//			replacements.add(new ReplacementInformation(current, row.indexOfChild(current)));
//		}
//		
//		row.addv
//		for (ReplacementInformation replacement: replacements) {
//			View old = row.getChildAt(replacement.index);
//			replacedViews.append(old.getId(),old);
//			row.removeViewAt(replacement.index);
//			row.addView(replacement.view, replacement.index);			
//		}
//		
//		// Turn off edit button while already editing		
//		original.setVisibility(View.GONE);
//		row.addView(btnDone, row.indexOfChild(original));
//			
//		// Set listener so that when "done" is selected on the IME, editing is stopped
//		editCaption.setOnEditorActionListener(new OnEditorActionListener() {			
//			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//		            if (actionId == EditorInfo.IME_ACTION_DONE) {
//		                stopEditing();
//		            	return true;
//		            }
//		            return false;
//		        }			
//		});
//		// Hide the caption text view
//		txtCaption.setVisibility(View.GONE);		
//		
//		row.addView(editCaption, 0);
//		// Make sure that edit text can be reached using the dpad: When navigating to the row, focus the edittext
//		row.setOnFocusChangeListener(new OnFocusChangeListener() {
//			public void onFocusChange(View v, boolean hasFocus) {
//				if (hasFocus)
//					editCaption.requestFocus();
//				
//			}
//		});
//		
//		// Grab focus for this EditText and display the soft input window
//		editCaption.requestFocus();	
//		final InputMethodManager mgr = (InputMethodManager) row.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//		mgr.showSoftInput(editCaption, 0);
//		
//		if (listener != null)
//			listener.onEditStart(row);
//	}
//	
//	/**
//	 * Puts the editor's row out of editing mode, that is hides the EditText and shows the 
//	 * TextView, setting its text property to reflect the changes entered by the user.
//	 * @return The new text value
//	 */
//	public String stopEditing() {
//		if (!isEditing())
//			return null;
//		
//		String result;
//		
//		ViewGroup row = getRow();
//    	InputMethodManager mgr = (InputMethodManager) row.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//		mgr.hideSoftInputFromWindow(row.getWindowToken(), 0);
//		
//		TextView txtCaption = (TextView) row.findViewById(R.id.txt_caption); 
//    	EditText editCaption = (EditText) row.findViewById(R.id.edit_caption);
//    	ImageView btnDone = (ImageView) row.findViewById(R.id.btn_done);
//    	
//    	result = editCaption.getText().toString();
//		txtCaption.setText(result);		
//		MarginLayoutParams layoutParams = (MarginLayoutParams) txtCaption.getLayoutParams();
////		layoutParams.leftMargin = oldMarginLeft;
////		layoutParams.topMargin = oldMarginTop;
////		layoutParams.rightMargin = oldMarginRight;
////		layoutParams.bottomMargin = oldMarginBottom;
////		txtCaption.setLayoutParams(layoutParams);
//		
//		txtCaption.setVisibility(View.VISIBLE);
//		
//		row.removeView(editCaption);
//		row.removeView(btnDone);
//		row.findViewById(R.id.btn_edit).setVisibility(View.VISIBLE);		
//		
//		if (listener != null)
//			listener.onEditStop(row, result);
//		return result;
//	}
//
//	public ViewGroup getRow() {
//		return row;
//	}
//	
//	public void setRow(ViewGroup row) {
//		if (this.row != row && isEditing())
//			stopEditing();
//		this.row = row;
//	}
//	
//	public void setRowEditorListener(RowEditorListener listener) {
//		this.listener = listener;
//	}
//	
//	public boolean isEditing() {
//		return row != null && row.findViewById(R.id.edit_caption) != null;
//	}
}
