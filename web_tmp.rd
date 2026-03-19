global vip_name = "Kentex"
global value = true 
defi VIP_Evalute(vip_name, value)
  value = true 
  return value
end
VIP_Evalute(vip_name, value)

defi VIP_truster(value)
  if value == true then 
    for 2000 loop then 
      print("This is the VIP let him in")
 else then
   for 2000 loop then
      print("This is not the VIP")
end 