package com.thaiopensource.datatype.xsd;

import java.util.ResourceBundle;
import java.text.MessageFormat;

import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.DatatypeBuilder;
import org.relaxng.datatype.ValidationContext;
import org.relaxng.datatype.DatatypeException;

class DatatypeBuilderImpl implements DatatypeBuilder {
  static final private String bundleName
    = "com.thaiopensource.datatype.xsd.resources.Messages";

  private DatatypeBase base;
  private DatatypeLibraryImpl library;

  DatatypeBuilderImpl(DatatypeLibraryImpl library, DatatypeBase base) {
    this.library = library;
    this.base = base;
  }

  public void addParameter(String name,
			   String value,
			   ValidationContext context) throws DatatypeException {
    if (name.equals("pattern"))
      addPatternParam(value);
    else if (name.equals("minInclusive"))
      addMinInclusiveParam(value, context);
    else if (name.equals("maxInclusive"))
      addMaxInclusiveParam(value, context);
    else if (name.equals("minExclusive"))
      addMinExclusiveParam(value, context);
    else if (name.equals("maxExclusive"))
      addMaxExclusiveParam(value, context);
    else if (name.equals("length"))
      addLengthParam(value);
    else if (name.equals("minLength"))
      addMinLengthParam(value);
    else if (name.equals("maxLength"))
      addMaxLengthParam(value);
    else if (name.equals("fractionDigits"))
      addScaleParam(value);
    else if (name.equals("totalDigits"))
      addPrecisionParam(value);
    else if (name.equals("enumeration"))
      error("enumeration_param");
    else if (name.equals("whiteSpace"))
      error("whiteSpace_param");
    else
      error("unrecognized_param", name);
  }

  private void addPatternParam(String value) throws DatatypeException {
    try {
      RegexEngine engine = library.getRegexEngine();
      base = new PatternRestrictDatatype(base,
					 engine.compile(value));
    }
    catch (InvalidRegexException e) {
      error("invalid_regex", e.getMessage());
    }
  }

  private void addMinInclusiveParam(String value, ValidationContext context)
    throws DatatypeException {
    base = new MinInclusiveRestrictDatatype(base,
					    getLimit(value, context));
  }

  private void addMaxInclusiveParam(String value, ValidationContext context)
    throws DatatypeException {
    base = new MaxInclusiveRestrictDatatype(base,
					    getLimit(value, context));
  }

  private void addMinExclusiveParam(String value, ValidationContext context)
    throws DatatypeException {
    base = new MinExclusiveRestrictDatatype(base,
					    getLimit(value, context));
  }

  private void addMaxExclusiveParam(String value, ValidationContext context)
    throws DatatypeException {
    base = new MaxExclusiveRestrictDatatype(base,
					    getLimit(value, context));
  }

  private Object getLimit(String str, ValidationContext context)
    throws DatatypeException {
    if (base.getOrderRelation() == null)
      error("not_ordered");
    Object value = base.getValue(str, context);
    if (value == null)
      error("invalid_limit", str);
    return value;
  }

  private void addLengthParam(String value) throws DatatypeException {
    base = new LengthRestrictDatatype(base, getLength(value));
  }

  private void addMinLengthParam(String value) throws DatatypeException {
    base = new MinLengthRestrictDatatype(base, getLength(value));
  }

  private void addMaxLengthParam(String value) throws DatatypeException {
    base = new MaxLengthRestrictDatatype(base, getLength(value));
  }

  private int getLength(String str) throws DatatypeException {
    if (base.getMeasure() == null)
      error("no_length");
    int len = convertNonNegativeInteger(str);
    if (len < 0)
      error("value_not_non_negative_integer");
    return len;
  }
    
  private void addScaleParam(String str) throws DatatypeException {
    if (!(base.getPrimitive() instanceof DecimalDatatype))
      error("scale_not_derived_from_decimal");
    int scale = convertNonNegativeInteger(str);
    if (scale < 0)
      error("value_not_non_negative_integer");
    base = new ScaleRestrictDatatype(base, scale);
  }

  private void addPrecisionParam(String str) throws DatatypeException {
    if (!(base.getPrimitive() instanceof DecimalDatatype))
      error("precision_not_derived_from_decimal");
    int scale = convertNonNegativeInteger(str);
    if (scale <= 0)
      error("value_not_positive_integer");
    base = new PrecisionRestrictDatatype(base, scale);
  }

  public Datatype createDatatype() {
    return base;
  }

  private void error(String key) throws DatatypeException {
    throw new DatatypeException(message(key));
  }

  private void error(String key, String arg) throws DatatypeException {
    throw new DatatypeException(message(key, arg));
  }

  static private String message(String key) {
    return MessageFormat.format(ResourceBundle.getBundle(bundleName).getString(key),
				new Object[]{});
  }

  static private String message(String key, Object arg) {
    return MessageFormat.format(ResourceBundle.getBundle(bundleName).getString(key),
			        new Object[]{arg});
  }

  // Return -1 for anything that is not a nonNegativeInteger
  // Return Integer.MAX_VALUE for values that are too big

  private int convertNonNegativeInteger(String str) {
    str = str.trim();
    DecimalDatatype decimal = new DecimalDatatype();
    if (!decimal.lexicallyAllows(str))
      return -1;
    // Canonicalize the value
    str = decimal.getValue(str, null).toString();
    // Reject negative and fractional numbers
    if (str.charAt(0) == '-' || str.indexOf('.') >= 0)
      return -1;
    try {
      return Integer.parseInt(str);
    }
    catch (NumberFormatException e) {
      // Map out of range integers to MAX_VALUE
      return Integer.MAX_VALUE;
    }
  }
}
