rootProject.name = "CustomTrader"
include(":BMLBuilder", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")