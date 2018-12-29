n_secondes=20
Num_Client=105


while [ "$Num_Client" -lt "700" ]; do


i=0
while [ "$i" -lt "5" ]; do


rm -rf /home/ayman/CompositionProject/'eclipse-workspace (CompositionEngine)'/RetrievedWSDLs/*
rm -rf /home/ayman/CompositionProject/'eclipse-workspace (Client)'/CompositionWSDLs/*

( java MyClient $Num_Client >>/home/ayman/CompositionProject/'eclipse-workspace (Client)'/Client/src/output.txt ) & sleep $n_secondes
kill $!
echo '===================================' >> /home/ayman/CompositionProject/'eclipse-workspace (Client)'/Client/src/output.txt

i=$(($i + 1))
done

echo '-----------------------------------' >> /home/ayman/CompositionProject/'eclipse-workspace (Client)'/Client/src/output.txt
Num_Client=$(($Num_Client + 10))

done
