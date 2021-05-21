rootProject.name = "CustomTrader"
include(":BMLBuilder", ":PlaceNpc", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":PlaceNpc").projectDir = file("../PlaceNpc")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")