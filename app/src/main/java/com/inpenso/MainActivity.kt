package com.inpenso

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFFB9F36A)
private val Panel = Color(0xFF1C2520)
private val Bg = Color(0xFF101612)
private val Muted = Color(0xFF8C9690)

data class Expense(val title:String, val category:String, val amount:Double, val date:String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InpensoApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InpensoApp(context:Context) {
    val prefs = remember { context.getSharedPreferences("inpenso", Context.MODE_PRIVATE) }
    var expenses by remember { mutableStateOf(readExpenses(prefs)) }
    var tab by remember { mutableStateOf("Home") }
    var adding by remember { mutableStateOf(false) }
    var budget by remember { mutableStateOf(prefs.getFloat("budget", 1500f).toDouble()) }
    val total = expenses.sumOf { it.amount }
    fun persist(list:List<Expense>) {
        val a = JSONArray()
        list.forEach { e -> a.put(JSONObject().put("title",e.title).put("category",e.category).put("amount",e.amount).put("date",e.date)) }
        prefs.edit().putString("expenses",a.toString()).apply()
    }
    MaterialTheme(colorScheme=darkColorScheme(primary=Green,background=Bg,surface=Panel,onPrimary=Color(0xFF111714))) {
        Scaffold(containerColor=Bg, bottomBar={
            NavigationBar(containerColor=Panel) {
                listOf("Home","Expenses","Analytics","Budgets").forEach { name ->
                    val ico = when(name) { "Home"->Icons.Default.Home; "Expenses"->Icons.Default.ReceiptLong; "Analytics"->Icons.Default.BarChart; else->Icons.Default.PieChart }
                    NavigationBarItem(selected=tab==name,onClick={tab=name},icon={Icon(ico,null)},label={Text(name)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Green,selectedTextColor=Green,indicatorColor=Color(0xFF34432D),unselectedIconColor=Muted,unselectedTextColor=Muted))
                }
            }
        }, floatingActionButton={ if(tab=="Home" || tab=="Expenses") FloatingActionButton(onClick={adding=true},containerColor=Green,contentColor=Color.Black){Icon(Icons.Default.Add,null)} }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top=22.dp,bottom=24.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("INPENSO",color=Green,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=3.sp)
                        Text(when(tab){"Home"->"Your finances";"Expenses"->"All expenses";"Analytics"->"Spending insights";else->"Your budgets"},fontSize=27.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    }
                    Icon(Icons.Default.AccountBalanceWallet,null,tint=Green,modifier=Modifier.size(30.dp))
                }
                when(tab) {
                    "Home" -> Home(total,budget,expenses)
                    "Expenses" -> ExpenseList(expenses)
                    "Analytics" -> Analytics(expenses,total)
                    else -> Budget(budget,total) { budget=it; prefs.edit().putFloat("budget",it.toFloat()).apply() }
                }
            }
        }
        if(adding) AddDialog({adding=false}) { e -> expenses=listOf(e)+expenses; persist(expenses); adding=false }
    }
}

private fun readExpenses(p:android.content.SharedPreferences):List<Expense> = try {
    val a=JSONArray(p.getString("expenses","[]"))
    (0 until a.length()).map { a.getJSONObject(it).let { o -> Expense(o.getString("title"),o.getString("category"),o.getDouble("amount"),o.getString("date")) } }
} catch(_:Exception) { emptyList() }

private fun cash(n:Double) = "$" + String.format(Locale.US,"%,.2f",n)

@Composable
fun Home(total:Double,budget:Double,expenses:List<Expense>) {
    Text(SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(Date()).replaceFirstChar { it.uppercase() },color=Muted)
    Spacer(Modifier.height(12.dp))
    Card(colors=CardDefaults.cardColors(containerColor=Green),shape=RoundedCornerShape(26.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("TOTAL SPENT",color=Color.Black.copy(alpha=.6f),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp)
            Text(cash(total),color=Color(0xFF111714),fontSize=38.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Column { Text("Monthly budget",color=Color.Black.copy(alpha=.6f)); Text(cash(budget),color=Color.Black,fontWeight=FontWeight.Bold) }
                Column(horizontalAlignment=Alignment.End) { Text("Remaining",color=Color.Black.copy(alpha=.6f)); Text(cash((budget-total).coerceAtLeast(0.0)),color=Color.Black,fontWeight=FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress={if(budget>0)(total/budget).toFloat().coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth().height(7.dp),color=Color.Black,trackColor=Color.Black.copy(alpha=.15f))
        }
    }
    Spacer(Modifier.height(24.dp))
    Text("Recent transactions",fontSize=18.sp,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    if(expenses.isEmpty()) Empty("No expenses yet","Tap + to add your first transaction")
    else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(expenses.take(6)){ExpenseItem(it)}}
}

@Composable
fun ExpenseList(expenses:List<Expense>) {
    if(expenses.isEmpty()) Empty("No expenses yet","Your transactions will appear here")
    else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(expenses){ExpenseItem(it)}}
}

@Composable
fun Analytics(expenses:List<Expense>,total:Double) {
    Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Total expenses",color=Muted)
            Text(cash(total),fontSize=34.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            expenses.groupBy{it.category}.mapValues{it.value.sumOf(Expense::amount)}.toList().sortedByDescending{it.second}.forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(pair.first);Text(cash(pair.second),color=Green)}
                LinearProgressIndicator(progress={if(total>0)(pair.second/total).toFloat() else 0f},modifier=Modifier.fillMaxWidth().height(5.dp),color=Green)
            }
            if(expenses.isEmpty()) Empty("No analytics yet","Add expenses to see your breakdown")
        }
    }
}

@Composable
fun Budget(budget:Double,total:Double,onSave:(Double)->Unit) {
    var input by remember(budget){mutableStateOf(budget.toString())}
    Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("MONTHLY LIMIT",color=Muted)
            Text(cash(budget),fontSize=34.sp,fontWeight=FontWeight.Bold)
            Text("Spent " + cash(total),color=Muted)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(input,{input=it},label={Text("New budget")},singleLine=true)
            Button(onClick={input.toDoubleOrNull()?.takeIf{it>0}?.let(onSave)},colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Color.Black)){Text("Save budget")}
        }
    }
}

@Composable
fun AddDialog(close:()->Unit,add:(Expense)->Unit) {
    var title by remember{mutableStateOf("")}
    var amount by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("Food")}
    AlertDialog(onDismissRequest=close,title={Text("New expense")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title,{title=it},label={Text("Name")},singleLine=true)
            OutlinedTextField(amount,{amount=it},label={Text("Amount")},singleLine=true)
            Text("Category",color=Muted)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Food","Transport","Shopping","Bills","Other").forEach{c->FilterChip(selected=category==c,onClick={category=c},label={Text(c)})}}
        }
    },confirmButton={TextButton(onClick={amount.toDoubleOrNull()?.takeIf{it>0}?.let{add(Expense(title.ifBlank{"Expense"},category,it,SimpleDateFormat("dd MMM",Locale.getDefault()).format(Date())))}}){Text("Add",color=Green)}},dismissButton={TextButton(onClick=close){Text("Cancel")}})
}

@Composable
fun ExpenseItem(e:Expense) {
    Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(17.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(Icons.Default.Payments,null,tint=Green,modifier=Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)){Text(e.title,fontWeight=FontWeight.SemiBold);Text(e.category+" · "+e.date,color=Muted,fontSize=12.sp)}
        Text("-"+cash(e.amount),fontWeight=FontWeight.Bold)
    }
}

@Composable
fun Empty(title:String,subtitle:String) {
    Column(Modifier.fillMaxWidth().padding(35.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(Icons.Default.ReceiptLong,null,tint=Muted,modifier=Modifier.size(34.dp))
        Text(title,fontWeight=FontWeight.SemiBold)
        Text(subtitle,color=Muted,fontSize=12.sp)
    }
}
