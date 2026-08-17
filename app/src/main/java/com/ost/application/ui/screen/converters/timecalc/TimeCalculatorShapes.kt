package com.ost.application.ui.screen.converters.timecalc
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object OperatorBadgeShapes {
    @Composable
    fun shapeFor(operator: Operator): Shape = when (operator) {
        Operator.DIVIDE -> MaterialShapes.Pill.toShape()
        Operator.MULTIPLY -> MaterialShapes.Clover8Leaf.toShape()
        Operator.ADD -> MaterialShapes.Clover4Leaf.toShape()
        Operator.SUBTRACT -> MaterialShapes.Pill.toShape()
    }
    fun rotationDegreesFor(operator: Operator): Float = when (operator) {
        Operator.ADD, Operator.SUBTRACT -> 45f
        Operator.DIVIDE, Operator.MULTIPLY -> 0f
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object KeypadButtonShapes {
    private val restingCorner = RoundedCornerShape(20.dp)
    private val pressedCorner = RoundedCornerShape(12.dp)
    private val operatorResting = RoundedCornerShape(20.dp)
    private val operatorPressed = RoundedCornerShape(28.dp)
    @Composable
    fun digit(): ButtonShapes = ButtonDefaults.shapes(shape = restingCorner, pressedShape = pressedCorner)
    @Composable
    fun operator(): ButtonShapes = ButtonDefaults.shapes(shape = operatorResting, pressedShape = operatorPressed)
    @Composable
    fun equal(): ButtonShapes = ButtonDefaults.shapes(shape = operatorResting, pressedShape = pressedCorner)
}
