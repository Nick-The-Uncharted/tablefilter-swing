/**
 * Author:  Luis M Pena  ( lu@coderazzi.net )
 * License: MIT License
 *
 * Copyright (c) 2007 Luis M. Pena  -  lu@coderazzi.net
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.coderazzi.filters.gui.editor;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.CellRendererPane;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import net.coderazzi.filters.IFilterTextParser;
import net.coderazzi.filters.gui.FilterSettings;


/**
 * Private interface, defining the editor component [usually a text field]<br>
 * There are two such implementations, the usual one, represented by a
 * {@link JTextField}, and a non-text-based one, which renders the content using
 * a {@link ListCellRenderer} component.<br>
 */
interface EditorComponent {

    /** Returns the swing component associated to the editor */
    public JComponent getComponent();

    /** 
     * Call always before {@link #getFilter()} to verify if the filter
     * has been updated. 
     * @param forceUpdate set to true if the filter must been updated, 
     *   even when the editor's content is not
     * @return the new filter  
     */
    public RowFilter checkFilterUpdate(boolean forceUpdate);

    /**
     * Returns the filter associated to the current content.<br> 
     * Always invoked after {@link #checkFilterUpdate(boolean)}
     */
    public RowFilter getFilter();

    /** 
     * Informs that the editor has received the focus.
     * @return true if the associated popup should be shown 
     */
    public boolean focusMoved(boolean gained);
    
    /** Enables/disables the editor */
    public void setEnabled(boolean enabled);

    /** Sets the {@link IFilterTextParser} associated to the editor */
    public void setTextParser(IFilterTextParser parser);

    /** Return the associated {@link IFilterTextParser}*/
    public IFilterTextParser getTextParser();

	/** 
	 * Sets the editor content<br>
	 * The parameters escapeIt must be true if the content could require
	 * escaping (otherwise, it will be always treated literally). <br>
	 */
    public void setContent(Object content, boolean escapeIt);

    /** Returns the definition associated to the current editor */
    public Object getContent();
    
    /** Reports the existing custom choices */
    public void reportCustomChoices(Collection<CustomChoice> customChoices);
    
    /** 
     * Defines the filter position associated to this editor.<br>
     * It corresponds to the table's model column
     **/
    public void setPosition(int position);

    /** Returns the filter position associated to this editor */
    public int getPosition();

    /** Sets the editable flag. If editable, the user can enter any content */
    public void setEditable(boolean set);

    /** Returns the editable flag*/
    public boolean isEditable();

    /** Sets the color used to show filter's errors (invalid syntax) */
    public void setErrorForeground(Color fg);

    /** Returns the color used to show filter's errors */
    public Color getErrorForeground();

    /** Sets the color used to represent disabled state */
    public void setDisabledForeground(Color fg);

    /** Returns the color used to represent disabled state */
    public Color getDisabledForeground();

    /** Sets the foreground color used to represent selected state */
    public void setSelectionForeground(Color fg);

    /** Sets the background color used to represent selected state */
    public void setSelectionBackground(Color bg);

    /** Sets the foreground color used*/
    public void setForeground(Color fg);

    /** Returns the foreground color used*/
    public Color getForeground();

    /**
     * EditorComponent for text edition, backed up with a {@link JTextField}<br>
     * It is editable by default.
     */
    static final class Text extends DocumentFilter 
    		implements DocumentListener, EditorComponent {

        private TextEditor textField = new TextEditor();
        private Collection<CustomChoice> customChoices = Collections.EMPTY_LIST;
        private IFilterTextParser parser;
        private int filterPosition;
        private boolean editable;
        private RowFilter filter;
        /*the source of the filter. Or a String, or a CustomChoice*/
        Object content; 
        PopupComponent popup;
        /** 
         * This variable is set to true if the content is being set from inside, 
         * to avoid raising some events 
         **/
        private boolean controlledSet;

        
        /**
         * Specific JTextField to be able to represent CustomChoices,
         * painting their associated icon on the background
         */
        class TextEditor extends JTextField implements CaretListener{
        	
			private static final long serialVersionUID = 3827985723062383846L;
			
			private Icon icon;
			private String trackedText;
	        private Color errorColor = Color.red;
	        private Color foreground, background;
	        private Color disabledColor;
	        private Color selectionBackground, selectionForeground;
	        private boolean enabled;
	        private boolean error;
	        private boolean focus;

        	public TextEditor() {
        		super(15); //created with 15 columns
        		addCaretListener(this);
			}
        	
        	@Override
        	public void setText(String t) {
        		String tmp = trackedText;
        		trackedText = null; //avoid any checks on the caret listener 
        		super.setText(t);
        		trackedText = tmp;
        	}
        	
        	/**
        	 * Activates, if possible, the custom decoration, that is,
        	 * if the content is a CustomChoice and has an associated icon
        	 */
        	public boolean activateCustomDecoration(){
        		boolean ret = false;
        		trackedText=null;
        		if (content instanceof CustomChoice){
        			icon = ((CustomChoice)content).getIcon();
        			if (icon!=null){
	            		trackedText=getText();
	            		ret=true;
        			}
        		} else {
        			icon=null;
        		}
        		ensureCorrectForegroundColor();
        		return ret;
        	}
        	
        	/** Deactivates the custom decoration */
        	public void deactivateCustomDecoration(){
        		if (icon!=null){
        			trackedText=null;
        			icon=null;
        			ensureCorrectForegroundColor();
        		}
        	}
        	
        	/**
        	 * Reports that the textfield has been enabled or not. Do not
        	 * call directly setEnabled().<br>
        	 * It sets the component as focusable, and 
        	 */
        	public void setEnabledState(boolean enabled){
        		//if enabled, there is already a check on the filter, so
        		//the activation / deactivation of decoration will
        		//work on its own
        		this.enabled=enabled;
        		if (!enabled && activateCustomDecoration()){
			    	icon = UIManager.getLookAndFeel().getDisabledIcon(this, icon);     
			    	trackedText = null; //do not track text changes
    			}
            	setFocusable(enabled);
        	}
        	
            public void focusMoved(boolean gained) {
            	focus=gained;
        		trackedText = null; // do not track changes (gain or not, not yet)
            	super.setBackground(gained? selectionBackground : background);
            	//whether we lose or gain focus, we try to reactivate the
            	//decoration -listeners only required if gaining it
            	//without focus, there is no need to check for changes on
            	//the text when painting the decoration, so set text to null
        		if (activateCustomDecoration() && !gained){
        			trackedText=null;
        		}
            }
        	
        	@Override
        	public void caretUpdate(CaretEvent e) {
                //if the user moves the cursor on the editor, the focus passes 
                //automatically back to the editor (from the popup)
        		if (enabled){
	                popup.setPopupFocused(false);
	                if (trackedText!=null){
	                	//if text is not null, there is decoration, remove it
	                	//(there can be decoration if text is null, but in that 
	                	// case the focus is not on the component, no need to
	                	// modify any decoration)
	                	deactivateCustomDecoration();
	                }
        		}
        	}
        	
        	public void setError(boolean error){
        		this.error=error;
        		if (enabled){
        			ensureCorrectForegroundColor();
        		}
        	}
        	
            public void setErrorForeground(Color fg) {
            	errorColor = fg;
            	if (error){
            		ensureCorrectForegroundColor();
            	}
            }

            public Color getErrorForeground() {
                return errorColor;
            }

            public void setDisabledForeground(Color fg) {
            	disabledColor = fg;
            	if (!enabled){
            		ensureCorrectForegroundColor();
            	}
            }

            public Color getDisabledForeground() {
                return disabledColor;
            }

            @Override public void setForeground(Color fg) {
            	foreground = fg;
            	ensureCorrectForegroundColor();
            }
            
            @Override public void setBackground(Color bg) {
            	background = bg;
            	super.setBackground(bg);
            }

            public Color getNormalForeground() {
                return foreground;
            }

            public void setSelectionForeground(Color fg) {
            	selectionForeground = fg;
            	if (focus){
            		ensureCorrectForegroundColor();
            	}
            }
            
            public void setSelectionBackground(Color bg) {
            	selectionBackground=bg;
            	if (focus){
            		super.setBackground(bg);
            	}
            }
            
            /** Ensures that the correct foreground is on use*/
            private void ensureCorrectForegroundColor(){
            	Color color;
            	if (enabled && icon==null){
            		if (error){
            			color = errorColor;
            		} else {
            			color = focus? selectionForeground : foreground;
            		}
            	} else {
            		color = disabledColor;
            	}
        		super.setForeground(color);
            }

			@Override protected void paintComponent(Graphics g) {
        		super.paintComponent(g);
        		if (icon!=null){
        			if (trackedText==null || trackedText.equals(getText())){
        			    int x=(getWidth()-icon.getIconWidth())/2;
        			    int y=(getHeight()-icon.getIconHeight())/2;    
    			    	icon.paintIcon(this, g, x, y);
        			} else {
        				deactivateCustomDecoration();
        			}
        		}
        	}
        }

        public Text(PopupComponent popupComponent) {
            this.popup = popupComponent;
            setEditable(true);
        }

        @Override public JComponent getComponent() {
            return textField;
        }

        @Override 
        public void reportCustomChoices(Collection<CustomChoice> customChoices) {
        	this.customChoices = customChoices;
        }

        @Override public RowFilter checkFilterUpdate(boolean forceUpdate) {
            String text = textField.getText().trim();
            if (forceUpdate){
            	updateFilter(text);
            } else if (content instanceof CustomChoice){
    			if (!((CustomChoice)content).getRepresentation().equals(text)){
                	updateFilter(text);
    			}
            } else if (!text.equals(content)){
            	updateFilter(text);            	
            }
            return filter;
        }
        
        /** Updates the filter / text is expected trimmed */
        private void updateFilter(String text) {
            boolean error=false;
            CustomChoice cc = getCustomChoice(text);
            if (cc!=null){
    			content = cc;
    			textField.activateCustomDecoration();
    			filter = cc.getFilter(parser, filterPosition);            	
            } else {
                content = text;
                filter = null;
                textField.deactivateCustomDecoration();
                try {
    	            if (!isEditable()){
    	            	//for editable columns, the text was already
    	            	//escaped when the content was defined
    	            	//(see setContent)
    	            	text = parser.escape(text, filterPosition);
    	            }
                	filter = parser.parseText(text, 
                						filterPosition);
                } catch (ParseException pex) {
                    error=true;
                }            	
            }
            textField.setError(error);
        }
        
        /** Returns the CustomChoice matching the text (trimmed) */
        private CustomChoice getCustomChoice(String text) {
        	if (text.length()==0){
        		return CustomChoice.MATCH_ALL;
        	}
        	Comparator<String> chooser = 
        		FilterSettings.getStringComparator(parser.isIgnoreCase());
        	for (CustomChoice cc : customChoices){
        		if (0==chooser.compare(text, cc.getRepresentation())){
        			return cc;
        		}
        	}
        	return null;
        }
        
        @Override public RowFilter getFilter() {
            return filter;
        }

        @Override public boolean focusMoved(boolean gained) {
    		textField.focusMoved(gained);
            return !editable;
        }

        @Override public void setEnabled(boolean enabled) {
        	textField.setEnabledState(enabled);
        }

        @Override public void setTextParser(IFilterTextParser parser) {
            this.parser = parser;
            if (textField.isEnabled()) {
                checkFilterUpdate(true);
            }
        }
        
        @Override public IFilterTextParser getTextParser() {
            return parser;
        }

        @Override public void setContent(Object content, boolean escapeIt) {
            String text;
            if (content instanceof CustomChoice){
            	//never escape custom choices
            	text = ((CustomChoice) content).getRepresentation();
            } else if (content instanceof String){
            	text = (String) content;
            	if (escapeIt && isEditable()){
            		//if no editable, the options is directly handled, no need
            		//to escape it. When the text is parsed, on no editable
            		//columns, it will be then escaped.
            		text = parser.escape(text, filterPosition);
            	}
            } else {
            	text = null;
            }
            setControlledSet();
            textField.setText(text);
        }

        @Override public void setPosition(int position) {
            this.filterPosition = position;
        }

        @Override public int getPosition() {
            return filterPosition;
        }

        @Override public Object getContent() {
        	checkFilterUpdate(false);
            return content;
        }

        @Override public void setEditable(boolean set) {
        	//dispose first the current listeners
            if (isEditable()) {
                textField.getDocument().removeDocumentListener(this);
            } else {
                ((AbstractDocument) textField.getDocument()).
                	setDocumentFilter(null);
            }
            editable = set;
            if (set) {
                textField.getDocument().addDocumentListener(this);
            } else {
                // ensure that the text contains something okay
                String proposal = getProposalOnEdition(textField.getText(), 
                		                               false);
                textField.setText((proposal == null) ? 
                		CustomChoice.MATCH_ALL.getRepresentation() 
                		: proposal);
                ((AbstractDocument) textField.getDocument()).
                	setDocumentFilter(this);
            }
            controlledSet = false;
        }

        @Override public boolean isEditable() {
            return editable;
        }

        @Override public void setErrorForeground(Color fg) {
        	textField.setErrorForeground(fg);        	
        }

        @Override public Color getErrorForeground() {
            return textField.getErrorForeground();
        }

        @Override public void setDisabledForeground(Color fg) {
        	textField.setDisabledForeground(fg);        	
        }

        @Override public Color getDisabledForeground() {
            return textField.getDisabledForeground();
        }

        @Override public void setForeground(Color fg) {
        	textField.setForeground(fg);
        }

        @Override public Color getForeground() {
            return textField.getNormalForeground();
        }

        @Override public void setSelectionForeground(Color fg) {
        	textField.setSelectionForeground(fg);
        }
        
        @Override public void setSelectionBackground(Color bg) {
        	textField.setSelectionBackground(bg);
        }
        
        /** defines that the content is being set from inside */
        public void setControlledSet() {
            controlledSet = true;
        }

        /** Returns a proposal for the current edition*/
        private String getProposalOnEdition(String hint, boolean perfectMatch) {
            String ret = popup.selectBestMatch(hint, perfectMatch);
            popup.setPopupFocused(false);
            return ret;
        }

        /** {@link DocumentFilter}: method called if handler is not editable */
        @Override
        public void insertString(FilterBypass fb,
                                 int offset,
                                 String string,
                                 AttributeSet attr) {
            // we never use it, we never invoke Document.insertString
            // note that normal (non programmatically) editing only invokes
            // replace/remove
        }

        /** {@link DocumentFilter}: method called if handler is not editable */
        @Override
        public void replace(FilterBypass fb,
                            int offset,
                            int length,
                            String text,
                            AttributeSet attrs) throws BadLocationException {
            String buffer = textField.getText();
            String newContentBegin = buffer.substring(0, offset) + text;
            String newContent = 
            	newContentBegin + buffer.substring(offset + length);
            String proposal = getProposalOnEdition(newContent, true);
            if (proposal == null) {
                // why this part? Imagine having text "se|cond" with the cursor
                // at "|". Nothing is selected.
                // if the user presses now 'c', the code above would imply
                // getting "seccond", which is probably wrong, so we try now
                // to get a proposal starting at 'sec' ['sec|ond']
                proposal = getProposalOnEdition(newContentBegin, true);
                if (proposal == null) {
                    return;
                }
                newContent = newContentBegin;
            }
            int caret;
            if (controlledSet) {
                controlledSet = false;
                caret = 0;
            } else {
                caret = 1 + Math.min(textField.getCaret().getDot(), 
                		             textField.getCaret().getMark());
            }
            super.replace(fb, 0, buffer.length(), proposal, attrs);
            textField.setCaretPosition(proposal.length());
            textField.moveCaretPosition(caret);
        }

        /** {@link DocumentFilter}: method called if handler is not editable */
        @Override
        public void remove(FilterBypass fb,
                           int offset,
                           int length) throws BadLocationException {
            int caret = textField.getCaret().getDot();
            int mark = textField.getCaret().getMark();
            String buffer = textField.getText();
            String newContent = buffer.substring(0, offset) 
            	+ buffer.substring(offset + length);
            String proposal = getProposalOnEdition(newContent, true);
            if (!newContent.equals(proposal)) {
                if (proposal == null) {
                    proposal = getProposalOnEdition(newContent, false);
                    if (proposal == null) {
                        return;
                    }
                }
                if (matchCount(proposal, newContent) 
                		<= matchCount(buffer, newContent)) {
                    proposal = buffer;
                }
            }
            super.replace(fb, 0, buffer.length(), proposal, null);
            
            //special case if the removal is due to BACK SPACE
    		AWTEvent ev = EventQueue.getCurrentEvent();
    		if ((ev instanceof KeyEvent) 
    				&& ((KeyEvent)ev).getKeyCode() == KeyEvent.VK_BACK_SPACE){
                if (caret > mark) {
                    caret = mark;
                } else if (buffer == proposal) {
                    --caret;
                } else if (caret == mark) {
                    caret = offset;
                }    			
    		} 
            textField.setCaretPosition(proposal.length());
            textField.moveCaretPosition(caret);
        }

        /** returns the number of starting chars matching among both pars */
        private int matchCount(String a,
                               String b) {
            int max = Math.min(a.length(), b.length());
            for (int i = 0; i < max; i++) {
                if (a.charAt(i) != b.charAt(i)) {
                    return i;
                }
            }
            return max;
        }

        /** {@link DocumentListener}: method called when handler is editable */
        @Override
		public void changedUpdate(DocumentEvent e) {
            // no need to handle updates
        }

        /** {@link DocumentListener}: method called when handler is editable */
        @Override
		public void removeUpdate(DocumentEvent e) {
            getProposalOnEdition(textField.getText(), false);
        }

        /** {@link DocumentListener}: method called when handler is editable */
        @Override
		public void insertUpdate(DocumentEvent e) {
            getProposalOnEdition(textField.getText(), false);
        }

    }

    /**
     * Editor component for cell rendering
     */
    static final class Rendered extends JComponent implements EditorComponent {

        private static final long serialVersionUID = -7162028817569553287L;

        private Object content = CustomChoice.MATCH_ALL;
        private FilterListCellRenderer renderer;
        private CellRendererPane painter = new CellRendererPane();
        private IFilterTextParser parser;
        private RowFilter filter;
        private Color errorColor;
        private Color disabledColor;
        private boolean focused;
        Object cachedContent;
        int filterPosition;

        public Rendered(FilterListCellRenderer renderer) {
            this.renderer = renderer;
        }
        
        @Override public JComponent getComponent() {
            return this;
        }
        
        @Override 
        public void reportCustomChoices(Collection<CustomChoice> customChoices) {
        	//a renderer does not need to know about these choices
        }

        @Override public RowFilter checkFilterUpdate(boolean forceUpdate) {
            Object currentContent = getContent();
            if (forceUpdate || (currentContent != cachedContent)) {
	            cachedContent = currentContent;
	            if (cachedContent instanceof CustomChoice) {
	                filter = ((CustomChoice)cachedContent).getFilter(parser, filterPosition);
	            } else {
	                filter = new RowFilter() {
	                        @Override
	                        public boolean include(RowFilter.Entry entry) {
	                            Object val = entry.getValue(filterPosition);
	                            return (val == null) ? (cachedContent == null) 
	                            		: val.equals(cachedContent);
	                        }
	                    };
	            }
            }
            return filter;
        }

        @Override public RowFilter getFilter() {
            return filter;
        }

        @Override public boolean focusMoved(boolean gained) {
        	focused=gained;
        	repaint();
            return true;
        }

        @Override public void setEnabled(boolean enabled) {
        	renderer.setEnabled(enabled);
        }

        @Override public void setTextParser(IFilterTextParser parser) {
            this.parser = parser;
            cachedContent = null;
        }

        @Override public IFilterTextParser getTextParser() {
            return parser;
        }
      
        @Override public void setContent(Object content, boolean escapeIt) {
            this.content = (content == null) ? CustomChoice.MATCH_ALL : content;
            repaint();
        }

        @Override public Object getContent() {
            return content;
        }

        @Override public void setPosition(int position) {
            this.filterPosition = position;
        }

        @Override public int getPosition() {
            return filterPosition;
        }

        @Override public void setEditable(boolean set) {
            // cannot apply to the rendered component -at least, not currently
        }

        @Override public boolean isEditable() {
            return false;
        }

        @Override public void setErrorForeground(Color fg) {
            this.errorColor = fg;
        }

        @Override public Color getErrorForeground() {
            return errorColor;
        }

        @Override public void setDisabledForeground(Color fg) {
            disabledColor = fg;
        }
        
        @Override public void setSelectionForeground(Color fg) {
        	//no need to do anything
        }
        
        @Override public void setSelectionBackground(Color bg) {
        	//no need to do anything
        }
        
        @Override public Color getDisabledForeground() {
            return disabledColor;
        }

        @Override public boolean isShowing() {
            return true;
        }

        @Override protected void paintComponent(Graphics g) {
            Component c = renderer.getCellRendererComponent(content, getWidth(),
            		focused);
            painter.paintComponent(g, c, this, 0, 0, getWidth(), getHeight());
        }
    }
}