import re

with open("atest_standard.result", "r") as standardFile:
	standardAllOut = standardFile.read()
	valid_sample = True

	try:
		standardOut = standardAllOut.split("STATISTICS")[0]
		std_cycle = int(standardAllOut.split("Total Cycles = ")[1].split("OTHER")[0].strip())
	except:
		
		valid_sample = False

	with open("atest_student.result", "r") as studentFile:
		studentAllOut = studentFile.read()
		studentOut = studentAllOut.split("STATISTICS")[0]

		if studentOut == standardOut and valid_sample:
			cycle = int(studentAllOut.split("Total Cycles = ")[1].split("OTHER")[0].strip())
			
			print("PASSED")
			print("sample cycle: " + str(std_cycle))
			print("your cycle: " + str(cycle))
			print()
		elif valid_sample:
			print("FAILED")
			print()
			print("Standard Output: ")
			print(standardOut)
			print("Your Output: ")
			print(studentOut)
			print()
		else:
			print("N/A")
			print("sample compiler has a problem")			
			print("Your Output: ")
			print(studentOut)
			print()
		



