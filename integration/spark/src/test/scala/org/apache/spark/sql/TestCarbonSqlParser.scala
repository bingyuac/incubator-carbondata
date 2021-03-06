package org.apache.spark.sql

import org.apache.spark.sql.common.util.QueryTest
import org.apache.spark.sql.execution.command.Field

/**
  * Stub class for calling the CarbonSqlParser
  */
private class TestCarbonSqlParserStub extends CarbonSqlParser {

  //val parser:CarbonSqlDDLParser = new CarbonSqlDDLParser()

  def updateColumnGroupsInFieldTest(fields: Seq[Field], tableProperties: Map[String, String]): Seq[String] = {

     var (dims: Seq[Field], noDictionaryDims: Seq[String]) = extractDimColsAndNoDictionaryFields(
      fields, tableProperties)
    val msrs: Seq[Field] = extractMsrColsFromFields(fields, tableProperties)

    updateColumnGroupsInField(tableProperties,
        noDictionaryDims, msrs, dims)
  }

  def extractDimColsAndNoDictionaryFieldsTest(fields: Seq[Field], tableProperties: Map[String, String]): (Seq[Field],
    Seq[String]) = {

    extractDimColsAndNoDictionaryFields(fields, tableProperties)
  }

  def extractMsrColsFromFieldsTest(fields: Seq[Field], tableProperties: Map[String, String]): (Seq[Field]) = {

    extractMsrColsFromFields(fields, tableProperties)
  }


}

/**
  * Test class to test Carbon Sql Parser
  */
class TestCarbonSqlParser extends QueryTest {

  /**
    * load all test fields
    * @return
    */
  def loadAllFields: Seq[Field] = {
    var fields: Seq[Field] = Seq[Field]()

    var col1 = Field("col1", Option("Int"), Option("col1"), None, null, Some("columnar"))
    var col2 = Field("col2", Option("String"), Option("col2"), None, null, Some("columnar"))
    var col3 = Field("col3", Option("String"), Option("col3"), None, null, Some("columnar"))
    var col4 = Field("col4", Option("Int"), Option("col4"), None, null, Some("columnar"))
    var col5 = Field("col5", Option("String"), Option("col5"), None, null, Some("columnar"))
    var col6 = Field("col6", Option("String"), Option("col6"), None, null, Some("columnar"))
    var col7 = Field("col7", Option("String"), Option("col7"), None, null, Some("columnar"))
    var col8 = Field("col8", Option("String"), Option("col8"), None, null, Some("columnar"))

    fields :+= col1
    fields :+= col2
    fields :+= col3
    fields :+= col4
    fields :+= col5
    fields :+= col6
    fields :+= col7
    fields :+= col8
    fields
  }

  // Testing the column group Splitting method.
  test("Test-updateColumnGroupsInField") {
    val colGroupStr = "(col2,col3),(col5,col6),(col7,col8)"
    val tableProperties = Map("COLUMN_GROUPS" -> colGroupStr)
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    val colgrps = stub.updateColumnGroupsInFieldTest(fields, tableProperties)
    assert(colgrps.lift(0).get.equalsIgnoreCase("col2,col3"))
    assert(colgrps.lift(1).get.equalsIgnoreCase("col5,col6"))
    assert(colgrps.lift(2).get.equalsIgnoreCase("col7,col8"))

  }
  test("Test-ColumnGroupsInvalidField_Shouldnotallow") {
    val colGroupStr = "(col1,col2),(col10,col6),(col7,col8)"
    val tableProperties = Map("COLUMN_GROUPS" -> colGroupStr)
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    try {
      val colgrps = stub.updateColumnGroupsInFieldTest(fields, tableProperties)
      assert(false)
    } catch {
      case e: Exception => assert(true)
    }
  }
  test("Test-MeasureInColumnGroup_ShouldNotAllow") {
    //col1 is measure
    val colGroupStr = "(col1,col2),(col5,col6),(col7,col8)"
    val tableProperties = Map("COLUMN_GROUPS" -> colGroupStr)
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    try {
      val colgrps = stub.updateColumnGroupsInFieldTest(fields, tableProperties)
      assert(false)
    } catch {
      case e: Exception => assert(true)
    }
  }
  test("Test-NoDictionaryInColumnGroup_ShouldNotAllow") {
    //col5 is no dictionary
    val colGroupStr = "(col2,col3),(col5,col6),(col7,col8)"
    val noDictStr = "col5"
    val tableProperties = Map("COLUMN_GROUPS" -> colGroupStr, "DICTIONARY_EXCLUDE" -> noDictStr)
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    try {
      val colgrps = stub.updateColumnGroupsInFieldTest(fields, tableProperties)
      assert(false)
    } catch {
      case e: Exception => assert(true)
    }
  }
  test("Test-SameColumnInDifferentGroup_ShouldNotAllow") {
    val colGroupStr = "(col2,col3),(col5,col6),(col6,col7,col8)"
    val tableProperties = Map("COLUMN_GROUPS" -> colGroupStr)
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    try {
      val colgrps = stub.updateColumnGroupsInFieldTest(fields, tableProperties)
      assert(false)
    } catch {
      case e: Exception => assert(true)
    }
  }
  
   test("Test-ColumnAreNotTogetherAsInSchema_ShouldNotAllow") {
    val colGroupStr = "(col2,col3),(col5,col8)"
    val tableProperties = Map("COLUMN_GROUPS" -> colGroupStr)
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    try {
      val colgrps = stub.updateColumnGroupsInFieldTest(fields, tableProperties)
      assert(false)
    } catch {
      case e: Exception => assert(true)
    }
  }
  test("Test-ColumnInColumnGroupAreShuffledButInSequence") {
    val colGroupStr = "(col2,col3),(col7,col8,col6)"
    val tableProperties = Map("COLUMN_GROUPS" -> colGroupStr)
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    
    val colgrps = stub.updateColumnGroupsInFieldTest(fields, tableProperties)
    assert(colgrps.lift(0).get.equalsIgnoreCase("col2,col3"))
    assert(colgrps.lift(1).get.equalsIgnoreCase("col6,col7,col8"))
  }
  // Testing the column group Splitting method with empty table properties so null will be returned.
  test("Test-Empty-updateColumnGroupsInField") {
    val tableProperties = Map("" -> "")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    val colgrps = stub.updateColumnGroupsInFieldTest(fields, Map())
    //assert( rtn === 1)
    assert(null == colgrps)
  }

  // Testing the extracting of Dims and no Dictionary
  test("Test-extractDimColsAndNoDictionaryFields") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col2", "DICTIONARY_INCLUDE" -> "col4")
    var fields: Seq[Field] = loadAllFields

    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub.extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)

    // testing col

    //All dimension fields should be available in dimensions list
    assert(dimCols.size == 7)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col3"))
    assert(dimCols.lift(2).get.column.equalsIgnoreCase("col4"))

    //No dictionary column names will be available in noDictionary list
    assert(noDictionary.size == 1)
    assert(noDictionary.lift(0).get.equalsIgnoreCase("col2"))
  }

  test("Test-DimAndMsrColsWithNoDictionaryFields1") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col1")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub
      .extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    //below fields should be available in dimensions list
    assert(dimCols.size == 7)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col1"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(2).get.column.equalsIgnoreCase("col3"))

    //below column names will be available in noDictionary list
    assert(noDictionary.size == 1)
    assert(noDictionary.lift(0).get.equalsIgnoreCase("col1"))

    //check msr
    assert(msrCols.size == 1)
    assert(msrCols.lift(0).get.column.equalsIgnoreCase("col4"))
  }

  test("Test-DimAndMsrColsWithNoDictionaryFields2") {
    val tableProperties = Map("DICTIONARY_INCLUDE" -> "col1")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub
      .extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    //below dimension fields should be available in dimensions list
    assert(dimCols.size == 7)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col1"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(2).get.column.equalsIgnoreCase("col3"))

    //below column names will be available in noDictionary list
    assert(noDictionary.size == 0)

    //check msr
    assert(msrCols.size == 1)
    assert(msrCols.lift(0).get.column.equalsIgnoreCase("col4"))
  }

  test("Test-DimAndMsrColsWithNoDictionaryFields3") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col1", "DICTIONARY_INCLUDE" -> "col4")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub
      .extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    //below dimension fields should be available in dimensions list
    assert(dimCols.size == 8)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col1"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(2).get.column.equalsIgnoreCase("col3"))
    assert(dimCols.lift(3).get.column.equalsIgnoreCase("col4"))

    //below column names will be available in noDictionary list
    assert(noDictionary.size == 1)
    assert(noDictionary.lift(0).get.equalsIgnoreCase("col1"))

    //check msr
    assert(msrCols.size == 0)
  }

  test("Test-DimAndMsrColsWithNoDictionaryFields4") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col3", "DICTIONARY_INCLUDE" -> "col2")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub
      .extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    //below dimension fields should be available in dimensions list
    assert(dimCols.size == 6)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col3"))

    //below column names will be available in noDictionary list
    assert(noDictionary.size == 1)
    assert(noDictionary.lift(0).get.equalsIgnoreCase("col3"))

    //check msr
    assert(msrCols.size == 2)
    assert(msrCols.lift(0).get.column.equalsIgnoreCase("col1"))
    assert(msrCols.lift(1).get.column.equalsIgnoreCase("col4"))
  }

  test("Test-DimAndMsrColsWithNoDictionaryFields5") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col4", "DICTIONARY_INCLUDE" -> "col2")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub
      .extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    //below dimension fields should be available in dimensions list
    assert(dimCols.size == 7)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col3"))
    assert(dimCols.lift(2).get.column.equalsIgnoreCase("col4"))

    //below column names will be available in noDictionary list
    assert(noDictionary.size == 1)
    assert(noDictionary.lift(0).get.equalsIgnoreCase("col4"))

    //check msr
    assert(msrCols.size == 1)
    assert(msrCols.lift(0).get.column.equalsIgnoreCase("col1"))
  }

  test("Test-DimAndMsrColsWithNoDictionaryFields6") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col2", "DICTIONARY_INCLUDE" -> "col1")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub
      .extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    //below dimension fields should be available in dimensions list
    assert(dimCols.size == 7)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col1"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(2).get.column.equalsIgnoreCase("col3"))

    //below column names will be available in noDictionary list
    assert(noDictionary.size == 1)
    assert(noDictionary.lift(0).get.equalsIgnoreCase("col2"))

    //check msr
    assert(msrCols.size == 1)
    assert(msrCols.lift(0).get.column.equalsIgnoreCase("col4"))
  }

  test("Test-DimAndMsrColsWithNoDictionaryFields7") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col2 ,col1  ",
      "DICTIONARY_INCLUDE" -> "col3 ,col4 "
    )
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub
      .extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    //below dimension fields should be available in dimensions list
    assert(dimCols.size == 8)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col1"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(2).get.column.equalsIgnoreCase("col3"))
    assert(dimCols.lift(3).get.column.equalsIgnoreCase("col4"))

    //below column names will be available in noDictionary list
    assert(noDictionary.size == 2)
    assert(noDictionary.lift(0).get.equalsIgnoreCase("col1"))
    assert(noDictionary.lift(1).get.equalsIgnoreCase("col2"))

    //check msr
    assert(msrCols.size == 0)
  }

  test("Test-DimAndMsrColsWithNoDictionaryFields8") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col2,col4", "DICTIONARY_INCLUDE" -> "col3")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var (dimCols, noDictionary) = stub
      .extractDimColsAndNoDictionaryFieldsTest(fields, tableProperties)
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    //below dimension fields should be available in dimensions list
    assert(dimCols.size == 7)
    assert(dimCols.lift(0).get.column.equalsIgnoreCase("col2"))
    assert(dimCols.lift(1).get.column.equalsIgnoreCase("col3"))
    assert(dimCols.lift(2).get.column.equalsIgnoreCase("col4"))

    //below column names will be available in noDictionary list
    assert(noDictionary.size == 2)
    assert(noDictionary.lift(0).get.equalsIgnoreCase("col2"))
    assert(noDictionary.lift(1).get.equalsIgnoreCase("col4"))

    //check msr
    assert(msrCols.size == 1)
    assert(msrCols.lift(0).get.column.equalsIgnoreCase("col1"))
  }

  // Testing the extracting of measures
  test("Test-extractMsrColsFromFields") {
    val tableProperties = Map("DICTIONARY_EXCLUDE" -> "col2", "DICTIONARY_INCLUDE" -> "col4")
    var fields: Seq[Field] = loadAllFields
    val stub = new TestCarbonSqlParserStub()
    var msrCols = stub.extractMsrColsFromFieldsTest(fields, tableProperties)

    // testing col
    assert(msrCols.lift(0).get.column.equalsIgnoreCase("col1"))

  }


}


