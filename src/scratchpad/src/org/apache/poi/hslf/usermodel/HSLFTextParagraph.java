/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hslf.usermodel;

import static org.apache.poi.hslf.record.RecordTypes.OutlineTextRefAtom;

import java.awt.Color;
import java.io.IOException;
import java.util.*;

import org.apache.poi.hslf.model.PPFont;
import org.apache.poi.hslf.model.textproperties.*;
import org.apache.poi.hslf.model.textproperties.TextPropCollection.TextPropType;
import org.apache.poi.hslf.record.*;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.util.*;

/**
 * This class represents a run of text in a powerpoint document. That
 *  run could be text on a sheet, or text in a note.
 *  It is only a very basic class for now
 *
 * @author Nick Burch
 */

public final class HSLFTextParagraph implements TextParagraph<HSLFTextRun> {
    protected static POILogger logger = POILogFactory.getLogger(HSLFTextParagraph.class);

    /**
     * How to align the text
     */
    /* package */ static final int AlignLeft = 0;
    /* package */ static final int AlignCenter = 1;
    /* package */ static final int AlignRight = 2;
    /* package */ static final int AlignJustify = 3;


    // Note: These fields are protected to help with unit testing
	//   Other classes shouldn't really go playing with them!
	private final TextHeaderAtom _headerAtom;
	private TextBytesAtom  _byteAtom;
	private TextCharsAtom  _charAtom;
	private final TextPropCollection _paragraphStyle = new TextPropCollection(1, TextPropType.paragraph);

    protected TextRulerAtom _ruler;
	protected List<HSLFTextRun> _runs = new ArrayList<HSLFTextRun>();
	protected HSLFTextShape _parentShape;
    private HSLFSheet _sheet;
    private int shapeId;

	// private StyleTextPropAtom styleTextPropAtom;
	private StyleTextProp9Atom styleTextProp9Atom;

	/**
    * Constructs a Text Run from a Unicode text block.
    * Either a {@link TextCharsAtom} or a {@link TextBytesAtom} needs to be provided.
    *
    * @param tha the TextHeaderAtom that defines what's what
    * @param tba the TextBytesAtom containing the text or null if {@link TextCharsAtom} is provided
    * @param tca the TextCharsAtom containing the text or null if {@link TextBytesAtom} is provided
	 */
	/* package */ HSLFTextParagraph(
        TextHeaderAtom tha,
        TextBytesAtom tba,
        TextCharsAtom tca
    ) {
	    if (tha == null)  {
	        throw new IllegalArgumentException("TextHeaderAtom must be set.");
	    }
		_headerAtom = tha;
        _byteAtom = tba;
        _charAtom = tca;
	}

    /* package */ HSLFTextParagraph(HSLFTextParagraph other) {
        _headerAtom = other._headerAtom;
        _byteAtom = other._byteAtom;
        _charAtom = other._charAtom;
        _parentShape = other._parentShape;
        _sheet = other._sheet;
        _ruler = other._ruler;
        shapeId = other.shapeId;
        _paragraphStyle.copy(other._paragraphStyle);
    }

	public void addTextRun(HSLFTextRun run) {
	    _runs.add(run);
	}

	/**
	 * Fetch the rich text runs (runs of text with the same styling) that
	 *  are contained within this block of text
	 */
	public List<HSLFTextRun> getTextRuns() {
		return 	_runs;
	}

	public TextPropCollection getParagraphStyle() {
	    return _paragraphStyle;
	}

	public void setParagraphStyle(TextPropCollection paragraphStyle) {
	    _paragraphStyle.copy(paragraphStyle);
	}

	/**
     * Supply the Sheet we belong to, which might have an assigned SlideShow
     * Also passes it on to our child RichTextRuns
     */
	public void supplySheet(HSLFSheet sheet){
        this._sheet = sheet;

        if (_runs == null) return;
        for(HSLFTextRun rt : _runs) {
            rt.updateSheet();
        }
	}

    public HSLFSheet getSheet(){
        return this._sheet;
    }

    /**
     * @return  Shape ID
     */
    protected int getShapeId(){
        return shapeId;
    }

    /**
     *  @param id Shape ID
     */
    protected void setShapeId(int id){
        shapeId = id;
    }

    /**
     * @return  0-based index of the text run in the SLWT container
     */
    protected int getIndex(){
        return (_headerAtom != null) ? _headerAtom.getIndex() : -1;
    }

    /**
     * Sets the index of the paragraph in the SLWT container
     *
     * @param index
     */
    protected void setIndex(int index) {
        if (_headerAtom != null) _headerAtom.setIndex(index);
    }

    /**
    * Returns the type of the text, from the TextHeaderAtom.
    * Possible values can be seen from TextHeaderAtom
    * @see org.apache.poi.hslf.record.TextHeaderAtom
    */
    public int getRunType() {
        return (_headerAtom != null) ? _headerAtom.getTextType() : -1;
    }

    /**
     * Is this Text Run one from a {@link PPDrawing}, or is it
     *  one from the {@link SlideListWithText}?
     */
    public boolean isDrawingBased() {
        return (getIndex() == -1);
    }

    public TextRulerAtom getTextRuler(){
        return _ruler;

    }

    public TextRulerAtom createTextRuler(){
        _ruler = getTextRuler();
        if (_ruler == null) {
            _ruler = TextRulerAtom.getParagraphInstance();
            Record childAfter = _byteAtom;
            if (childAfter == null) childAfter = _charAtom;
            if (childAfter == null) childAfter = _headerAtom;
            _headerAtom.getParentRecord().addChildAfter(_ruler, childAfter);
        }
        return _ruler;
    }

    /**
     * Returns records that make up the list of text paragraphs
     * (there can be misc InteractiveInfo, TxInteractiveInfo and other records)
     *
     * @return text run records
     */
    public Record[] getRecords(){
        Record r[] = _headerAtom.getParentRecord().getChildRecords();
        return getRecords(r, new int[]{0}, _headerAtom);
    }

    private static Record[] getRecords(Record[] records, int[] startIdx, TextHeaderAtom headerAtom) {
        if (records == null) {
            throw new NullPointerException("records need to be set.");
        }
        
        for (; startIdx[0] < records.length; startIdx[0]++) {
            Record r = records[startIdx[0]];
            if (r instanceof TextHeaderAtom && (headerAtom == null || r == headerAtom)) break;
        }

        if (startIdx[0] >= records.length) {
            logger.log(POILogger.INFO, "header atom wasn't found - container might contain only an OutlineTextRefAtom");
            return new Record[0];
        }
        
        int length;
        for (length = 1; startIdx[0]+length < records.length; length++) {
            if (records[startIdx[0]+length] instanceof TextHeaderAtom) break;
        }
        
        Record result[] = new Record[length];
        System.arraycopy(records, startIdx[0], result, 0, length);
        startIdx[0] += length;
        
        return result;
    }
    
    /** Numbered List info */
	public void setStyleTextProp9Atom(final StyleTextProp9Atom styleTextProp9Atom) {
		this.styleTextProp9Atom = styleTextProp9Atom;
	}

    /** Numbered List info */
	public StyleTextProp9Atom getStyleTextProp9Atom() {
		return this.styleTextProp9Atom;
	}

	/**
     * Fetch the value of the given Paragraph related TextProp.
     * Returns -1 if that TextProp isn't present.
     * If the TextProp isn't present, the value from the appropriate
     *  Master Sheet will apply.
     */
    private int getParaTextPropVal(String propName) {
        TextProp prop = _paragraphStyle.findByName(propName);
        BitMaskTextProp maskProp = (BitMaskTextProp)_paragraphStyle.findByName(ParagraphFlagsTextProp.NAME);
        boolean hardAttribute = (maskProp != null && maskProp.getValue() == 0);
        if (prop == null && !hardAttribute){
            HSLFSheet sheet = getSheet();
            int txtype = getRunType();
            HSLFMasterSheet master = sheet.getMasterSheet();
            if (master != null)
                prop = master.getStyleAttribute(txtype, getIndentLevel(), propName, false);
        }

        return prop == null ? -1 : prop.getValue();
    }

    /**
     * Sets the value of the given Character TextProp, add if required
     * @param propName The name of the Character TextProp
     * @param val The value to set for the TextProp
     */
    public void setParaTextPropVal(String propName, int val) {
        // Ensure we have the StyleTextProp atom we're going to need
        assert(_paragraphStyle!=null);
        TextProp tp = fetchOrAddTextProp(_paragraphStyle, propName);
        tp.setValue(val);
    }

    @Override
    public Iterator<HSLFTextRun> iterator() {
        return _runs.iterator();
    }

    @Override
    public double getLeftMargin() {
        int val = getParaTextPropVal("text.offset");
        return val*HSLFShape.POINT_DPI/((double)HSLFShape.MASTER_DPI);
    }

    @Override
    public void setLeftMargin(double leftMargin) {
        int val = (int)(leftMargin*HSLFShape.MASTER_DPI/HSLFShape.POINT_DPI);
        setParaTextPropVal("text.offset", val);
    }

    @Override
    public double getRightMargin() {
        // TODO: find out, how to determine this value
        return 0;
    }

    @Override
    public void setRightMargin(double rightMargin) {
        // TODO: find out, how to set this value
    }

    @Override
    public double getIndent() {
        int val = getParaTextPropVal("bullet.offset");
        return val*HSLFShape.POINT_DPI/((double)HSLFShape.MASTER_DPI);
    }

    @Override
    public void setIndent(double intent) {
        int val = (int)(intent*HSLFShape.MASTER_DPI/HSLFShape.POINT_DPI);
        setParaTextPropVal("bullet.offset", val);
    }

    @Override
    public String getDefaultFontFamily() {
        return (_runs.isEmpty() ? "Arial" : _runs.get(0).getFontFamily());
    }

    @Override
    public double getDefaultFontSize() {
        return (_runs.isEmpty() ? 12 : _runs.get(0).getFontSize());
    }

    /**
     * Sets the type of horizontal alignment for the paragraph.
     *
     * @param align - the type of alignment
     */
    public void setAlignment(org.apache.poi.sl.usermodel.TextParagraph.TextAlign align) {
        int alignInt;
        switch (align) {
        default:
        case LEFT: alignInt = TextAlignmentProp.LEFT; break;
        case CENTER: alignInt = TextAlignmentProp.CENTER; break;
        case RIGHT: alignInt = TextAlignmentProp.RIGHT; break;
        case DIST: alignInt = TextAlignmentProp.DISTRIBUTED; break;
        case JUSTIFY: alignInt = TextAlignmentProp.JUSTIFY; break;
        case JUSTIFY_LOW: alignInt = TextAlignmentProp.JUSTIFYLOW; break;
        case THAI_DIST: alignInt = TextAlignmentProp.THAIDISTRIBUTED; break;
        }
        setParaTextPropVal("alignment", alignInt);
    }

    @Override
    public org.apache.poi.sl.usermodel.TextParagraph.TextAlign getTextAlign() {
        switch (getParaTextPropVal("alignment")) {
            default:
            case TextAlignmentProp.LEFT: return TextAlign.LEFT;
            case TextAlignmentProp.CENTER: return TextAlign.CENTER;
            case TextAlignmentProp.RIGHT: return TextAlign.RIGHT;
            case TextAlignmentProp.JUSTIFY: return TextAlign.JUSTIFY;
            case TextAlignmentProp.JUSTIFYLOW: return TextAlign.JUSTIFY_LOW;
            case TextAlignmentProp.DISTRIBUTED: return TextAlign.DIST;
            case TextAlignmentProp.THAIDISTRIBUTED: return TextAlign.THAI_DIST;
        }
    }

    @Override
    public FontAlign getFontAlign() {
        switch(getParaTextPropVal("fontAlign")) {
            default:
            case -1: return FontAlign.AUTO;
            case FontAlignmentProp.BASELINE: return FontAlign.BASELINE;
            case FontAlignmentProp.TOP: return FontAlign.TOP;
            case FontAlignmentProp.CENTER: return FontAlign.CENTER;
            case FontAlignmentProp.BOTTOM: return FontAlign.BOTTOM;
        }
    }

    @Override
    public BulletStyle getBulletStyle() {
        if (getBulletChar() == 0) return null;

        return new BulletStyle() {
            public String getBulletCharacter() {
                char chr =  HSLFTextParagraph.this.getBulletChar();
                return (chr == 0 ? "" : ""+chr);
            }

            public String getBulletFont() {
                int fontIdx = HSLFTextParagraph.this.getBulletFont();
                if (fontIdx == -1) return getDefaultFontFamily();
                PPFont ppFont = getSheet().getSlideShow().getFont(fontIdx);
                return ppFont.getFontName();
            }

            public double getBulletFontSize() {
                return HSLFTextParagraph.this.getBulletSize();
            }

            public Color getBulletFontColor() {
                return HSLFTextParagraph.this.getBulletColor();
            }
        };
    }

    @Override
    public HSLFTextShape getParentShape() {
        return _parentShape;
    }

    public void setParentShape(HSLFTextShape parentShape) {
        _parentShape = parentShape;
    }


    /**
    *
    * @return indentation level
    */
   public int getIndentLevel() {
       return _paragraphStyle == null ? 0 : _paragraphStyle.getIndentLevel();
   }

   /**
    * Sets indentation level
    *
    * @param level indentation level. Must be in the range [0, 4]
    */
   public void setIndentLevel(int level) {
       if( _paragraphStyle != null ) _paragraphStyle.setIndentLevel((short)level);
   }

   /**
    * Sets whether this rich text run has bullets
    */
   public void setBullet(boolean flag) {
       setFlag(ParagraphFlagsTextProp.BULLET_IDX, flag);
   }

   /**
    * Returns whether this rich text run has bullets
    */
   public boolean isBullet() {
       return getFlag(ParagraphFlagsTextProp.BULLET_IDX);
   }

   /**
    * Returns whether this rich text run has bullets
    */
   public boolean isBulletHard() {
       return getFlag(ParagraphFlagsTextProp.BULLET_IDX);
   }

   /**
    * Sets the bullet character
    */
   public void setBulletChar(char c) {
       setParaTextPropVal("bullet.char", c);
   }

   /**
    * Returns the bullet character
    */
   public char getBulletChar() {
       int val = getParaTextPropVal("bullet.char");
       return (char)(val == -1 ? 0 : val);
   }

   /**
    * Sets the bullet size
    */
   public void setBulletSize(int size) {
       setParaTextPropVal("bullet.size", size);
   }

   /**
    * Returns the bullet size
    */
   public int getBulletSize() {
       return getParaTextPropVal("bullet.size");
   }

   /**
    * Sets the bullet color
    */
   public void setBulletColor(Color color) {
       int rgb = new Color(color.getBlue(), color.getGreen(), color.getRed(), 254).getRGB();
       setParaTextPropVal("bullet.color", rgb);
   }

   /**
    * Returns the bullet color
    */
   public Color getBulletColor() {
       int rgb = getParaTextPropVal("bullet.color");
       if (rgb == -1) {
           // if bullet color is undefined, return color of first run
           if (_runs.isEmpty()) return null;
           return _runs.get(0).getFontColor();
       }

       int cidx = rgb >> 24;
       if (rgb % 0x1000000 == 0){
           if (_sheet == null) return null;
           ColorSchemeAtom ca = _sheet.getColorScheme();
           if(cidx >= 0 && cidx <= 7) rgb = ca.getColor(cidx);
       }
       Color tmp = new Color(rgb, true);
       return new Color(tmp.getBlue(), tmp.getGreen(), tmp.getRed());
   }

   /**
    * Sets the bullet font
    */
   public void setBulletFont(int idx) {
       setParaTextPropVal("bullet.font", idx);
       setFlag(ParagraphFlagsTextProp.BULLET_HARDFONT_IDX, true);
   }

   /**
    * Returns the bullet font
    */
   public int getBulletFont() {
       return getParaTextPropVal("bullet.font");
   }

   @Override
   public void setLineSpacing(double lineSpacing) {
       // if lineSpacing < 0, we need to convert points (common interface) to master units (hslf)
       if (lineSpacing < 0) {
           lineSpacing = (lineSpacing*HSLFShape.MASTER_DPI/HSLFShape.POINT_DPI);
       }
       setParaTextPropVal("linespacing", (int)lineSpacing);
   }

   @Override
   public double getLineSpacing() {
       double val = getParaTextPropVal("linespacing");
       // if lineSpacing < 0, we need to convert master units (hslf) to points (common interface)
       if (val == -1) return 0;
       if (val < -1) val *= HSLFShape.POINT_DPI/((double)HSLFShape.MASTER_DPI);
       return val;
   }

   /**
    * Sets spacing before a paragraph.
    * <p>
    * If spacebefore >= 0, then spacebefore is a percentage of normal line height.
    * If spacebefore < 0, the absolute value of spacebefore is the spacing in master coordinates.
    * </p>
    */
   public void setSpaceBefore(int val) {
       setParaTextPropVal("spacebefore", val);
   }

   /**
    * Returns spacing before a paragraph
    * <p>
    * If spacebefore >= 0, then spacebefore is a percentage of normal line height.
    * If spacebefore < 0, the absolute value of spacebefore is the spacing in master coordinates.
    * </p>
    *
    * @return the spacing before a paragraph
    */
   @Override
   public double getSpaceBefore() {
       int val = getParaTextPropVal("spacebefore");
       return val == -1 ? 0 : val;
   }

   /**
    * Sets spacing after a paragraph.
    * <p>
    * If spaceafter >= 0, then spaceafter is a percentage of normal line height.
    * If spaceafter < 0, the absolute value of spaceafter is the spacing in master coordinates.
    * </p>
    */
   public void setSpaceAfter(int val) {
       setParaTextPropVal("spaceafter", val);
   }

   /**
    * Returns spacing after a paragraph
    * <p>
    * If spaceafter >= 0, then spaceafter is a percentage of normal line height.
    * If spaceafter < 0, the absolute value of spaceafter is the spacing in master coordinates.
    * </p>
    *
    * @return the spacing before a paragraph
    */
   @Override
   public double getSpaceAfter() {
       int val = getParaTextPropVal("spaceafter");
       return val == -1 ? 0 : val;
   }

   /**
    * Returns the named TextProp, either by fetching it (if it exists) or adding it
    *  (if it didn't)
    * @param textPropCol The TextPropCollection to fetch from / add into
    * @param textPropName The name of the TextProp to fetch/add
    */
    protected static TextProp fetchOrAddTextProp(TextPropCollection textPropCol, String textPropName) {
        // Fetch / Add the TextProp
        return textPropCol.addWithName(textPropName);
    }

    protected boolean getFlag(int index) {
        if (_paragraphStyle == null) return false;

        BitMaskTextProp prop = (BitMaskTextProp) _paragraphStyle.findByName(ParagraphFlagsTextProp.NAME);

        if (prop == null) {
            if (_sheet != null) {
                int txtype = getRunType();
                HSLFMasterSheet master = _sheet.getMasterSheet();
                if (master != null) {
                    prop = (BitMaskTextProp) master.getStyleAttribute(txtype, getIndentLevel(), ParagraphFlagsTextProp.NAME, false);
                }
            } else {
                logger.log(POILogger.WARN, "MasterSheet is not available");
            }
        }

        return prop == null ? false : prop.getSubValue(index);
    }

   protected void setFlag(int index, boolean value) {
       // Ensure we have the StyleTextProp atom we're going to need
       assert(_paragraphStyle!=null);
       BitMaskTextProp prop = (BitMaskTextProp) fetchOrAddTextProp(_paragraphStyle, ParagraphFlagsTextProp.NAME);
       prop.setSubValue(value,index);
   }

   /**
    * Check and add linebreaks to text runs leading other paragraphs
    *
    * @param paragraphs
    */
   protected static void fixLineEndings(List<HSLFTextParagraph> paragraphs) {
       HSLFTextRun lastRun = null;
       for (HSLFTextParagraph p : paragraphs) {
           if (lastRun != null && !lastRun.getRawText().endsWith("\r")) {
               lastRun.setText(lastRun.getRawText()+"\r");
           }
           List<HSLFTextRun> ltr = p.getTextRuns();
           if (ltr.isEmpty()) {
               throw new RuntimeException("paragraph without textruns found");
           }
           lastRun = ltr.get(ltr.size()-1);
           assert(lastRun.getRawText() != null);
       }
   }

   /**
    * Search for a StyleTextPropAtom is for this text header (list of paragraphs)
    * 
    * @param header the header
    * @param textLen the length of the rawtext, or -1 if the length is not known
    */
   private static StyleTextPropAtom findStyleAtomPresent(TextHeaderAtom header, int textLen) {
       boolean afterHeader = false;
       StyleTextPropAtom style = null;
       for (Record record : header.getParentRecord().getChildRecords()) {
           long rt = record.getRecordType();
           if (afterHeader && rt == RecordTypes.TextHeaderAtom.typeID) {
               // already on the next header, quit searching
               break;
           }
           afterHeader |= (header == record);
           if (afterHeader && rt == RecordTypes.StyleTextPropAtom.typeID) {
               // found it
               style = (StyleTextPropAtom)record;
           }
       }

       if (style == null) {
           logger.log(POILogger.INFO, "styles atom doesn't exist. Creating dummy record for later saving.");
           style = new StyleTextPropAtom((textLen < 0) ? 1 : textLen);
       } else {
           if (textLen >= 0) {
               style.setParentTextSize(textLen);
           }
       }
       
       return style;
   }


   /**
    * Saves the modified paragraphs/textrun to the records.
    * Also updates the styles to the correct text length.
    */
   protected static void storeText(List<HSLFTextParagraph> paragraphs) {
       fixLineEndings(paragraphs);

       String rawText = toInternalString(getRawText(paragraphs));

       // Will it fit in a 8 bit atom?
       boolean isUnicode = StringUtil.hasMultibyte(rawText);
       // isUnicode = true;

       TextHeaderAtom headerAtom = paragraphs.get(0)._headerAtom;
       TextBytesAtom byteAtom = paragraphs.get(0)._byteAtom;
       TextCharsAtom charAtom = paragraphs.get(0)._charAtom;
       StyleTextPropAtom styleAtom = findStyleAtomPresent(headerAtom, rawText.length());

       // Store in the appropriate record
       Record oldRecord = null, newRecord = null;
       if (isUnicode) {
           if (byteAtom != null || charAtom == null) {
               oldRecord = byteAtom;
               charAtom = new TextCharsAtom();
           }
           newRecord = charAtom;
           charAtom.setText(rawText);
       } else {
           if (charAtom != null || byteAtom == null) {
               oldRecord = charAtom;
               byteAtom = new TextBytesAtom();
           }
           newRecord = byteAtom;
           byte[] byteText = new byte[rawText.length()];
           StringUtil.putCompressedUnicode(rawText,byteText,0);
           byteAtom.setText(byteText);
       }
       assert(newRecord != null);
       
       RecordContainer _txtbox = headerAtom.getParentRecord();
       Record[] cr = _txtbox.getChildRecords();
       int headerIdx = -1, textIdx = -1, styleIdx = -1;
       for (int i=0; i<cr.length; i++) {
           Record r = cr[i];
           if (r == headerAtom) headerIdx = i;
           else if (r == oldRecord || r == newRecord) textIdx = i;
           else if (r == styleAtom) styleIdx = i;
       }

       if (textIdx == -1) {
           // the old record was never registered, ignore it
           _txtbox.addChildAfter(newRecord, headerAtom);
           textIdx = headerIdx+1;
       } else {
           // swap not appropriated records - noop if unchanged
           cr[textIdx] = newRecord;
       }

       if (styleIdx == -1) {
           // Add the new StyleTextPropAtom after the TextCharsAtom / TextBytesAtom
           _txtbox.addChildAfter(styleAtom, newRecord);
       }
       
       for (HSLFTextParagraph p : paragraphs) {
           if (newRecord == byteAtom) {
               p._byteAtom = byteAtom;
               p._charAtom = null;
           } else {
               p._byteAtom = null;
               p._charAtom = charAtom;
           }
       }
       
       // Update the text length for its Paragraph and Character stylings
       //   * reset the length, to the new string's length
       //   * add on +1 if the last block

       styleAtom.clearStyles();

       TextPropCollection lastPTPC = null, lastRTPC = null, ptpc = null, rtpc = null;
       for (HSLFTextParagraph para : paragraphs) {
           ptpc = para.getParagraphStyle();
           ptpc.updateTextSize(0);
           if (!ptpc.equals(lastPTPC)) {
               lastPTPC = styleAtom.addParagraphTextPropCollection(0);
               lastPTPC.copy(ptpc);
           }
           for (HSLFTextRun tr : para.getTextRuns()) {
               rtpc = tr.getCharacterStyle();
               rtpc.updateTextSize(0);
               if (!rtpc.equals(lastRTPC)) {
                   lastRTPC = styleAtom.addCharacterTextPropCollection(0);
                   lastRTPC.copy(rtpc);
               }
               int len = tr.getLength();
               ptpc.updateTextSize(ptpc.getCharactersCovered()+len);
               rtpc.updateTextSize(len);
               lastPTPC.updateTextSize(lastPTPC.getCharactersCovered()+len);
               lastRTPC.updateTextSize(lastRTPC.getCharactersCovered()+len);
           }
       }

       assert(lastPTPC != null && lastRTPC != null && ptpc != null && rtpc != null);
       ptpc.updateTextSize(ptpc.getCharactersCovered()+1);
       rtpc.updateTextSize(rtpc.getCharactersCovered()+1);
       lastPTPC.updateTextSize(lastPTPC.getCharactersCovered()+1);
       lastRTPC.updateTextSize(lastRTPC.getCharactersCovered()+1);

       /**
        * If TextSpecInfoAtom is present, we must update the text size in it,
        * otherwise the ppt will be corrupted
        */
       for (Record r : paragraphs.get(0).getRecords()) {
           if (r instanceof TextSpecInfoAtom) {
               ((TextSpecInfoAtom)r).setParentSize(rawText.length()+1);
               break;
           }
       }
       
       if (_txtbox instanceof EscherTextboxWrapper) {
           try {
               ((EscherTextboxWrapper)_txtbox).writeOut(null);
           } catch (IOException e) {
               throw new RuntimeException("failed dummy write", e);
           }
       }
   }

   /**
    * Adds the supplied text onto the end of the TextParagraphs,
    * creating a new RichTextRun for it to sit in.
    *
    * @param text the text string used by this object.
    */
   protected static HSLFTextRun appendText(List<HSLFTextParagraph> paragraphs, String text, boolean newParagraph) {
       text = toInternalString(text);

       // check paragraphs
       assert(!paragraphs.isEmpty() && !paragraphs.get(0).getTextRuns().isEmpty());

       HSLFTextParagraph htp = paragraphs.get(paragraphs.size()-1);
       HSLFTextRun htr = htp.getTextRuns().get(htp.getTextRuns().size()-1);

       boolean isFirst = !newParagraph;
       for (String rawText : text.split("(?<=\r)")) {
           if (!isFirst) {
               TextPropCollection tpc = htp.getParagraphStyle();
               HSLFTextParagraph prevHtp = htp;
               htp = new HSLFTextParagraph(htp._headerAtom, htp._byteAtom, htp._charAtom);
               htp.getParagraphStyle().copy(tpc);
               htp.setParentShape(prevHtp.getParentShape());
               htp.setShapeId(prevHtp.getShapeId());
               htp.supplySheet(prevHtp.getSheet());
               paragraphs.add(htp);
           }
           isFirst = false;
           
           TextPropCollection tpc = htr.getCharacterStyle();
           // special case, last text run is empty, we will reuse it
           if (htr.getLength() > 0) {
               htr = new HSLFTextRun(htp);
               htr.getCharacterStyle().copy(tpc);
               htp.addTextRun(htr);
           }
           htr.setText(rawText);
       }
       
       storeText(paragraphs);

       return htr;
   }

   /**
    * Sets (overwrites) the current text.
    * Uses the properties of the first paragraph / textrun
    *
    * @param text the text string used by this object.
    */
   public static HSLFTextRun setText(List<HSLFTextParagraph> paragraphs, String text) {
       // check paragraphs
       assert(!paragraphs.isEmpty() && !paragraphs.get(0).getTextRuns().isEmpty());

       Iterator<HSLFTextParagraph> paraIter = paragraphs.iterator();
       HSLFTextParagraph htp = paraIter.next(); // keep first
       assert(htp != null);
       while (paraIter.hasNext()) {
           paraIter.next();
           paraIter.remove();
       }

       Iterator<HSLFTextRun> runIter = htp.getTextRuns().iterator();
       HSLFTextRun htr = runIter.next();
       htr.setText("");
       assert(htr != null);
       while (runIter.hasNext()) {
           runIter.next();
           runIter.remove();
       }

       return appendText(paragraphs, text, false);
   }

   public static String getText(List<HSLFTextParagraph> paragraphs) {
       assert(!paragraphs.isEmpty());
       String rawText = getRawText(paragraphs);
       return toExternalString(rawText, paragraphs.get(0).getRunType());
   }
   
   public static String getRawText(List<HSLFTextParagraph> paragraphs) {
       StringBuilder sb = new StringBuilder();
       for (HSLFTextParagraph p : paragraphs) {
           for (HSLFTextRun r : p.getTextRuns()) {
               sb.append(r.getRawText());
           }
       }
       return sb.toString();
   }

   /**
    * Returns a new string with line breaks converted into internal ppt
    * representation
    */
   protected static String toInternalString(String s) {
       String ns = s.replaceAll("\\r?\\n", "\r");
       return ns;
   }

   /**
    * Converts raw text from the text paragraphs to a formatted string,
    * i.e. it converts certain control characters used in the raw txt
    *
    * @param rawText the raw text
    * @param runType the run type of the shape, paragraph or headerAtom.
    *        use -1 if unknown
    * @return the formatted string
    */
   public static String toExternalString(String rawText, int runType) {
       // PowerPoint seems to store files with \r as the line break
       // The messes things up on everything but a Mac, so translate
       // them to \n
       String text = rawText.replace('\r', '\n');

       switch (runType) {
           // 0xB acts like cariage return in page titles and like blank in the
           // others
           case -1:
           case org.apache.poi.hslf.record.TextHeaderAtom.TITLE_TYPE:
           case org.apache.poi.hslf.record.TextHeaderAtom.CENTER_TITLE_TYPE:
               text = text.replace((char) 0x0B, '\n');
               break;
           default:
               text = text.replace((char) 0x0B, ' ');
               break;
       }

       return text;
   }

   /**
    * For a given PPDrawing, grab all the TextRuns
    */
   public static List<List<HSLFTextParagraph>> findTextParagraphs(PPDrawing ppdrawing, HSLFSheet sheet) {
       List<List<HSLFTextParagraph>> runsV = new ArrayList<List<HSLFTextParagraph>>();
       for (EscherTextboxWrapper wrapper : ppdrawing.getTextboxWrappers()) {
           runsV.add(findTextParagraphs(wrapper, sheet));
       }
       return runsV;
   }

   /**
    * Scans through the supplied record array, looking for
    * a TextHeaderAtom followed by one of a TextBytesAtom or
    * a TextCharsAtom. Builds up TextRuns from these
    *
    * @param wrapper an EscherTextboxWrapper
    */
   protected static List<HSLFTextParagraph> findTextParagraphs(EscherTextboxWrapper wrapper, HSLFSheet sheet) {
       // propagate parents to parent-aware records
       RecordContainer.handleParentAwareRecords(wrapper);
       int shapeId = wrapper.getShapeId();
       List<HSLFTextParagraph> rv = null;
       
       OutlineTextRefAtom ota = (OutlineTextRefAtom)wrapper.findFirstOfType(OutlineTextRefAtom.typeID);
       if (ota != null) {
           // if we are based on an outline, there are no further records to be parsed from the wrapper
           if (sheet == null) {
               throw new RuntimeException("Outline atom reference can't be solved without a sheet record");
           }
           
           List<List<HSLFTextParagraph>> sheetRuns = sheet.getTextParagraphs();
           assert(sheetRuns != null);
           
           int idx = ota.getTextIndex();
           for (List<HSLFTextParagraph> r : sheetRuns) {
               if (r.isEmpty()) continue;
               int ridx = r.get(0).getIndex();
               if (ridx > idx) break;
               if (ridx == idx) {
                   if (rv == null) {
                       rv = r;
                   } else {
                       // create a new container
                       // TODO: ... is this case really happening?
                       rv = new ArrayList<HSLFTextParagraph>(rv);
                       rv.addAll(r);
                   }
               }
           }
           if(rv == null || rv.isEmpty()) {
               logger.log(POILogger.WARN, "text run not found for OutlineTextRefAtom.TextIndex=" + idx);
           }
       } else {
           if (sheet != null) {
               // check sheet runs first, so we get exactly the same paragraph list
               List<List<HSLFTextParagraph>> sheetRuns = sheet.getTextParagraphs();
               assert(sheetRuns != null);

               for (List<HSLFTextParagraph> paras : sheetRuns) {
                   if (!paras.isEmpty() && paras.get(0)._headerAtom.getParentRecord() == wrapper) {
                       rv = paras;
                       break;
                   }
               }
           }
           
           if (rv == null) {
               // if we haven't found the wrapper in the sheet runs, create a new paragraph list from its record
               List<List<HSLFTextParagraph>> rvl = findTextParagraphs(wrapper.getChildRecords());
               switch (rvl.size()) {
               case 0: break; // nothing found
               case 1: rv = rvl.get(0); break; // normal case
               default:
                   throw new RuntimeException("TextBox contains more than one list of paragraphs.");
               }
           }
       }
       
       if (rv !=  null) {
           StyleTextProp9Atom styleTextProp9Atom = wrapper.getStyleTextProp9Atom();
           
           for (HSLFTextParagraph htp : rv) {
               htp.setShapeId(shapeId);
               htp.setStyleTextProp9Atom(styleTextProp9Atom);
           }
       }
       return rv;
   }

   /**
    * Scans through the supplied record array, looking for
    * a TextHeaderAtom followed by one of a TextBytesAtom or
    * a TextCharsAtom. Builds up TextRuns from these
    *
    * @param records the records to build from
    */
    protected static List<List<HSLFTextParagraph>> findTextParagraphs(Record[] records) {
        List<List<HSLFTextParagraph>> paragraphCollection = new ArrayList<List<HSLFTextParagraph>>();

        int[] recordIdx = {0};
        
        for (int slwtIndex = 0; recordIdx[0] < records.length; slwtIndex++) {
            TextHeaderAtom    header = null;
            TextBytesAtom     tbytes = null;
            TextCharsAtom     tchars = null;
            TextRulerAtom     ruler  = null;
            MasterTextPropAtom indents = null;

            for (Record r : getRecords(records, recordIdx, null)) {
                long rt = r.getRecordType();
                if (RecordTypes.TextHeaderAtom.typeID == rt) {
                    header = (TextHeaderAtom)r;
                } else if (RecordTypes.TextBytesAtom.typeID == rt) {
                    tbytes = (TextBytesAtom)r;
                } else if (RecordTypes.TextCharsAtom.typeID == rt) {
                    tchars = (TextCharsAtom)r;
                } else if (RecordTypes.TextRulerAtom.typeID == rt) {
                    ruler = (TextRulerAtom)r;
                } else if (RecordTypes.MasterTextPropAtom.typeID == rt) {
                    indents = (MasterTextPropAtom)r;
                }
                // don't search for RecordTypes.StyleTextPropAtom.typeID here ... see findStyleAtomPresent below
            }

            if (header == null) break;
            
            if (header.getParentRecord() instanceof SlideListWithText) {
                // runs found in PPDrawing are not linked with SlideListWithTexts
                header.setIndex(slwtIndex);
            }

            if (tbytes == null && tchars == null) {
                tbytes = new TextBytesAtom();
                // don't add record yet - set it in storeText
                logger.log(POILogger.INFO, "bytes nor chars atom doesn't exist. Creating dummy record for later saving.");
            }

            String rawText = (tchars != null) ? tchars.getText() : tbytes.getText();
            StyleTextPropAtom styles = findStyleAtomPresent(header, rawText.length());

            List<HSLFTextParagraph> paragraphs = new ArrayList<HSLFTextParagraph>();
            paragraphCollection.add(paragraphs);
            
            // split, but keep delimiter
            for (String para : rawText.split("(?<=\r)")) {
                HSLFTextParagraph tpara = new HSLFTextParagraph(header, tbytes, tchars);
                paragraphs.add(tpara);
                tpara._ruler = ruler;
                tpara.getParagraphStyle().updateTextSize(para.length());

                HSLFTextRun trun = new HSLFTextRun(tpara);
                tpara.addTextRun(trun);
                trun.setText(para);
            }

            applyCharacterStyles(paragraphs, styles.getCharacterStyles());
            applyParagraphStyles(paragraphs, styles.getParagraphStyles());
            if (indents != null) {
                applyParagraphIndents(paragraphs, indents.getIndents());
            }
        }

        if (paragraphCollection.isEmpty()) {
            logger.log(POILogger.DEBUG, "No text records found.");
        }
        
        return paragraphCollection;
    }

    protected static void applyCharacterStyles(List<HSLFTextParagraph> paragraphs, List<TextPropCollection> charStyles) {
        int paraIdx = 0, runIdx = 0;
        HSLFTextRun trun;

        for (int csIdx=0; csIdx<charStyles.size(); csIdx++) {
            TextPropCollection p = charStyles.get(csIdx);
            for (int ccRun = 0, ccStyle = p.getCharactersCovered(); ccRun < ccStyle; ) {
                HSLFTextParagraph para = paragraphs.get(paraIdx);
                List<HSLFTextRun> runs = para.getTextRuns();
                trun = runs.get(runIdx);
                int len = trun.getLength();

                if (ccRun+len <= ccStyle) {
                    ccRun += len;
                } else {
                    String text = trun.getRawText();
                    trun.setText(text.substring(0,ccStyle-ccRun));

                    HSLFTextRun nextRun = new HSLFTextRun(para);
                    nextRun.setText(text.substring(ccStyle-ccRun));
                    runs.add(runIdx+1, nextRun);

                    ccRun += ccStyle-ccRun;
                }

                TextPropCollection pCopy = new TextPropCollection(0, TextPropType.character);
                pCopy.copy(p);
                trun.setCharacterStyle(pCopy);

                len = trun.getLength();
                if (paraIdx == paragraphs.size()-1 && runIdx == runs.size()-1) {
                    if (csIdx < charStyles.size()-1) {
                        // special case, empty trailing text run
                        HSLFTextRun nextRun = new HSLFTextRun(para);
                        nextRun.setText("");
                        runs.add(nextRun);
                    } else {
                        // need to add +1 to the last run of the last paragraph
                        len++;
                        ccRun++;
                    }
                }
                pCopy.updateTextSize(len);

                // need to compare it again, in case a run has been added after
                if (++runIdx == runs.size()) {
                    paraIdx++;
                    runIdx = 0;
                }
            }
        }
    }

    protected static void applyParagraphStyles(List<HSLFTextParagraph> paragraphs, List<TextPropCollection> paraStyles) {
        int paraIdx = 0;
        for (TextPropCollection p : paraStyles) {
            for (int ccPara = 0, ccStyle = p.getCharactersCovered(); ccPara < ccStyle; paraIdx++) {
                if (paraIdx >= paragraphs.size() || ccPara >= ccStyle-1) return;
                HSLFTextParagraph htp = paragraphs.get(paraIdx);
                TextPropCollection pCopy = new TextPropCollection(0, TextPropType.paragraph);
                pCopy.copy(p);
                htp.setParagraphStyle(pCopy);
                int len = 0;
                for (HSLFTextRun trun : htp.getTextRuns()) {
                    len += trun.getLength();
                }
                if (paraIdx == paragraphs.size()-1) len++;
                pCopy.updateTextSize(len);
                ccPara += len;
            }
        }
    }

    protected static void applyParagraphIndents(List<HSLFTextParagraph> paragraphs, List<IndentProp> paraStyles) {
        int paraIdx = 0;
        for (IndentProp p : paraStyles) {
            for (int ccPara = 0, ccStyle = p.getCharactersCovered(); ccPara < ccStyle; paraIdx++) {
                HSLFTextParagraph para = paragraphs.get(paraIdx);
                int len = 0;
                for (HSLFTextRun trun : para.getTextRuns()) {
                    len += trun.getLength();
                }
                para.setIndentLevel(p.getIndentLevel());
                ccPara += len+1;
            }
        }
    }

    protected static List<HSLFTextParagraph> createEmptyParagraph() {
        EscherTextboxWrapper wrapper = new EscherTextboxWrapper();

        TextHeaderAtom tha = new TextHeaderAtom();
        tha.setParentRecord(wrapper);
        wrapper.appendChildRecord(tha);

        TextBytesAtom tba = new TextBytesAtom();
        tba.setText("".getBytes());
        wrapper.appendChildRecord(tba);

        StyleTextPropAtom sta = new StyleTextPropAtom(1);
        TextPropCollection paraStyle = sta.addParagraphTextPropCollection(1);
        TextPropCollection charStyle = sta.addCharacterTextPropCollection(1);
        wrapper.appendChildRecord(sta);

        HSLFTextParagraph htp = new HSLFTextParagraph(tha, tba, null);
        htp.setParagraphStyle(paraStyle);

        HSLFTextRun htr = new HSLFTextRun(htp);
        htr.setCharacterStyle(charStyle);
        htr.setText("");
        htp.addTextRun(htr);

        return Arrays.asList(htp);
    }

    public EscherTextboxWrapper getTextboxWrapper() {
        return (EscherTextboxWrapper)_headerAtom.getParentRecord();
    }
}
