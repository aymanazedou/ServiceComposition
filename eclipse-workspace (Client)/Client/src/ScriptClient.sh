n_secondes=20
Num_Client=5

rm -rf /home/ayman/CompositionProject/'eclipse-workspace (CompositionEngine)'/RetrievedWSDLs/*
rm -rf /home/ayman/CompositionProject/'eclipse-workspace (Client)'/CompositionWSDLs/*

while [ "$Num_Client" -lt "1000" ]; do

( java MyClient $Num_Client >>/home/ayman/CompositionProject/'eclipse-workspace (Client)'/Client/src/outputSUITE2.txt ) & sleep $n_secondes

kill $!
echo $Num_Client ' DONE'
sleep 3

rm -rf /home/ayman/CompositionProject/'eclipse-workspace (CompositionEngine)'/RetrievedWSDLs/*
rm -rf /home/ayman/CompositionProject/'eclipse-workspace (Client)'/CompositionWSDLs/*


n_secondes=$(($n_secondes + 1))
Num_Client=$(($Num_Client + 5))

done
